package com.seoulaim.domain.order.service;

import com.seoulaim.domain.goods.dto.ProductOrderSnapshot;
import com.seoulaim.domain.goods.service.ProductService;
import com.seoulaim.domain.order.dto.CreateOrderRequest;
import com.seoulaim.domain.order.dto.CreateOrderResponse;
import com.seoulaim.domain.order.dto.OrderDetailResponse;
import com.seoulaim.domain.order.dto.OrderItemInfo;
import com.seoulaim.domain.order.dto.OrderItemRequest;
import com.seoulaim.domain.order.dto.OrderSummaryResponse;
import com.seoulaim.domain.order.entity.ContactType;
import com.seoulaim.domain.order.entity.Order;
import com.seoulaim.domain.order.entity.OrderItem;
import com.seoulaim.domain.order.entity.OrderStatus;
import com.seoulaim.domain.order.entity.Payment;
import com.seoulaim.domain.order.entity.PaymentStatus;
import com.seoulaim.domain.order.entity.PgProvider;
import com.seoulaim.domain.order.repository.OrderItemRepository;
import com.seoulaim.domain.order.repository.OrderRepository;
import com.seoulaim.domain.order.repository.PaymentRepository;
import com.seoulaim.global.config.OrderProperties;
import com.seoulaim.global.config.TossPaymentProperties;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

  private static final List<String> SEOUL_PREFIXES = List.of("서울특별시", "서울", "Seoul");
  private static final DateTimeFormatter ORDER_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
  private static final String ORDER_NUMBER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final int ORDER_NUMBER_RANDOM_LENGTH = 6;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final PaymentRepository paymentRepository;
  private final ProductService productService;
  private final OrderProperties orderProperties;
  private final TossPaymentProperties tossPaymentProperties;
  private final Clock clock;

  @Transactional
  public CreateOrderResponse create(UUID userId, CreateOrderRequest request) {
    validateSeoulAddress(request.addressLine1());
    Map<UUID, ProductOrderSnapshot> products = loadProducts(request.items());

    long totalProductAmount =
        request.items().stream()
            .mapToLong(item -> products.get(item.productId()).price() * item.quantity())
            .sum();
    Instant now = clock.instant();
    Order order =
        orderRepository.save(
            Order.createPending(
                generateOrderNumber(now),
                userId,
                totalProductAmount,
                orderProperties.shippingFee(),
                request.recipientName(),
                request.addressLine1(),
                request.addressLine2(),
                request.postalCode(),
                request.contactType() == null ? null : ContactType.valueOf(request.contactType()),
                request.contactValue(),
                now));

    orderItemRepository.saveAll(
        request.items().stream()
            .map(
                item -> {
                  ProductOrderSnapshot product = products.get(item.productId());
                  return OrderItem.create(
                      order.getId(),
                      product.id(),
                      product.name(),
                      product.price(),
                      item.quantity());
                })
            .toList());
    // 토스 orderId 로 주문번호를 쓰므로 멱등키도 주문번호로 둔다
    paymentRepository.save(
        Payment.createReady(
            order.getId(), PgProvider.TOSS, order.getOrderNumber(), order.getFinalAmount()));

    return new CreateOrderResponse(
        order.getId(),
        order.getOrderNumber(),
        order.getFinalAmount(),
        tossPaymentProperties.clientKey());
  }

  public List<OrderSummaryResponse> getMyOrders(UUID userId) {
    List<Order> orders = orderRepository.findTop50ByUserIdOrderByOrderedAtDescIdDesc(userId);
    if (orders.isEmpty()) {
      return List.of();
    }
    Map<UUID, List<OrderItem>> itemsByOrderId =
        orderItemRepository
            .findAllByOrderIdInOrderByIdAsc(orders.stream().map(Order::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(OrderItem::getOrderId));
    return orders.stream()
        .map(
            order ->
                OrderSummaryResponse.from(
                    order, itemsByOrderId.getOrDefault(order.getId(), List.of())))
        .toList();
  }

  public OrderDetailResponse getMyOrder(UUID userId, UUID orderId) {
    Order order =
        orderRepository
            .findByIdAndUserId(orderId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    List<OrderItem> items = orderItemRepository.findAllByOrderIdInOrderByIdAsc(List.of(orderId));
    PaymentStatus paymentStatus =
        paymentRepository.findByOrderId(orderId).map(Payment::getStatus).orElse(null);
    return OrderDetailResponse.of(order, items, paymentStatus);
  }

  /** 결제 완료된 내 주문 중 다이어리 상품 줄. 다이어리 제출 여부는 diary 도메인이 거른다. */
  public List<OrderItemInfo> getPaidDiaryItems(UUID userId) {
    List<Order> orders =
        orderRepository.findAllByUserIdAndStatusOrderByOrderedAtDescIdDesc(
            userId, OrderStatus.PAID);
    if (orders.isEmpty()) {
      return List.of();
    }
    List<OrderItem> items =
        orderItemRepository.findAllByOrderIdInOrderByIdAsc(
            orders.stream().map(Order::getId).toList());
    Set<UUID> diaryProductIds = findDiaryProductIds(items);
    List<OrderItem> diaryItems =
        items.stream().filter(item -> diaryProductIds.contains(item.getProductId())).toList();
    return toItemInfos(orders, diaryItems);
  }

  /** 내 결제 완료 주문의 다이어리 상품 줄이면 반환한다. 남의 주문·미결제·일반 상품이면 empty. */
  public Optional<OrderItemInfo> findPaidDiaryItem(UUID userId, UUID orderItemId) {
    OrderItem item = orderItemRepository.findById(orderItemId).orElse(null);
    if (item == null) {
      return Optional.empty();
    }
    Optional<Order> order =
        orderRepository.findByIdAndUserId(item.getOrderId(), userId).filter(Order::isPaid);
    if (order.isEmpty() || findDiaryProductIds(List.of(item)).isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(toItemInfos(List.of(order.get()), List.of(item)).getFirst());
  }

  /** 주문 상품 줄을 주문번호와 함께 조회한다. 없는 id 는 결과에서 빠진다. */
  public List<OrderItemInfo> getOrderItems(Collection<UUID> orderItemIds) {
    if (orderItemIds.isEmpty()) {
      return List.of();
    }
    List<OrderItem> items = orderItemRepository.findAllById(orderItemIds);
    List<Order> orders =
        orderRepository.findAllById(items.stream().map(OrderItem::getOrderId).distinct().toList());
    return toItemInfos(orders, items);
  }

  private Set<UUID> findDiaryProductIds(List<OrderItem> items) {
    List<UUID> productIds = items.stream().map(OrderItem::getProductId).distinct().toList();
    return productService.getOrderSnapshots(productIds).stream()
        .filter(ProductOrderSnapshot::isDiary)
        .map(ProductOrderSnapshot::id)
        .collect(Collectors.toSet());
  }

  private List<OrderItemInfo> toItemInfos(List<Order> orders, List<OrderItem> items) {
    if (items.isEmpty()) {
      return List.of();
    }
    Map<UUID, Order> ordersById =
        orders.stream().collect(Collectors.toMap(Order::getId, Function.identity()));
    // 결제 승인 전에는 approvedAt 이 null 이라 toMap 을 쓸 수 없다
    Map<UUID, Instant> paidAtByOrderId = new HashMap<>();
    paymentRepository
        .findAllByOrderIdIn(ordersById.keySet())
        .forEach(payment -> paidAtByOrderId.put(payment.getOrderId(), payment.getApprovedAt()));
    return items.stream()
        .map(
            item -> {
              Order order = ordersById.get(item.getOrderId());
              return new OrderItemInfo(
                  item.getId(),
                  order.getId(),
                  order.getOrderNumber(),
                  item.getProductId(),
                  item.getProductName(),
                  paidAtByOrderId.get(order.getId()));
            })
        .toList();
  }

  private void validateSeoulAddress(String addressLine1) {
    String address = addressLine1.strip();
    // 외국인 사용자가 영문 주소를 쓰므로 대소문자를 무시한다
    if (SEOUL_PREFIXES.stream()
        .noneMatch(prefix -> address.regionMatches(true, 0, prefix, 0, prefix.length()))) {
      throw new BusinessException(ErrorCode.ORDER_ADDRESS_NOT_SEOUL);
    }
  }

  // 같은 상품이 여러 줄로 오면 다이어리 1개 제한을 우회할 수 있으므로 중복 자체를 막는다
  private Map<UUID, ProductOrderSnapshot> loadProducts(List<OrderItemRequest> items) {
    List<UUID> productIds = items.stream().map(OrderItemRequest::productId).toList();
    if (productIds.stream().distinct().count() != productIds.size()) {
      throw new BusinessException(ErrorCode.ORDER_INVALID_ITEM);
    }
    Map<UUID, ProductOrderSnapshot> products =
        productService.getOrderSnapshots(productIds).stream()
            .collect(Collectors.toMap(ProductOrderSnapshot::id, Function.identity()));
    for (OrderItemRequest item : items) {
      ProductOrderSnapshot product = products.get(item.productId());
      if (product == null || (product.isDiary() && item.quantity() > 1)) {
        throw new BusinessException(ErrorCode.ORDER_INVALID_ITEM);
      }
    }
    return products;
  }

  private String generateOrderNumber(Instant now) {
    StringBuilder random = new StringBuilder(ORDER_NUMBER_RANDOM_LENGTH);
    for (int i = 0; i < ORDER_NUMBER_RANDOM_LENGTH; i++) {
      random.append(ORDER_NUMBER_CHARS.charAt(RANDOM.nextInt(ORDER_NUMBER_CHARS.length())));
    }
    return "ORD-" + ORDER_DATE_FORMAT.format(now) + "-" + random;
  }
}
