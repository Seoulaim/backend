package com.seoulaim.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.goods.dto.ProductOrderSnapshot;
import com.seoulaim.domain.goods.service.ProductService;
import com.seoulaim.domain.order.dto.CreateOrderRequest;
import com.seoulaim.domain.order.dto.CreateOrderResponse;
import com.seoulaim.domain.order.dto.OrderItemRequest;
import com.seoulaim.domain.order.entity.Order;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-29T03:00:00Z");
  private static final UUID USER_ID = UUID.randomUUID();
  private static final ProductOrderSnapshot DOLL =
      new ProductOrderSnapshot(UUID.randomUUID(), "Doll", 15000L, false);
  private static final ProductOrderSnapshot DIARY =
      new ProductOrderSnapshot(UUID.randomUUID(), "Diary", 25000L, true);

  @Mock private OrderRepository orderRepository;
  @Mock private OrderItemRepository orderItemRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private ProductService productService;

  private OrderService orderService;

  @BeforeEach
  void setUp() {
    orderService =
        new OrderService(
            orderRepository,
            orderItemRepository,
            paymentRepository,
            productService,
            new OrderProperties(3000L),
            new TossPaymentProperties("test_sk", "test_ck"),
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private CreateOrderRequest request(String addressLine1, List<OrderItemRequest> items) {
    return new CreateOrderRequest(
        items, "Kim", addressLine1, "101호", "04524", "KAKAO", "seoul_fan");
  }

  @Test
  @DisplayName("주문을 생성하면 서버가 금액을 계산하고 PENDING 주문과 READY 결제를 저장한다")
  void create_정상요청_금액계산후저장() {
    given(productService.getOrderSnapshots(List.of(DOLL.id(), DIARY.id())))
        .willReturn(List.of(DOLL, DIARY));
    given(orderRepository.save(any(Order.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    CreateOrderResponse response =
        orderService.create(
            USER_ID,
            request(
                "서울특별시 중구 세종대로 110",
                List.of(new OrderItemRequest(DOLL.id(), 2), new OrderItemRequest(DIARY.id(), 1))));

    ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
    verify(orderRepository).save(orderCaptor.capture());
    Order order = orderCaptor.getValue();
    assertThat(order.getTotalProductAmount()).isEqualTo(15000L * 2 + 25000L);
    assertThat(order.getShippingFee()).isEqualTo(3000L);
    assertThat(order.getFinalAmount()).isEqualTo(58000L);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(order.getUserId()).isEqualTo(USER_ID);
    assertThat(order.getOrderedAt()).isEqualTo(NOW);
    assertThat(order.getOrderNumber()).matches("ORD-20261029-[A-Z0-9]{6}");

    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
    verify(paymentRepository).save(paymentCaptor.capture());
    Payment payment = paymentCaptor.getValue();
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
    assertThat(payment.getPgProvider()).isEqualTo(PgProvider.TOSS);
    assertThat(payment.getAmount()).isEqualTo(58000L);
    assertThat(payment.getIdempotencyKey()).isEqualTo(order.getOrderNumber());

    assertThat(response.orderId()).isEqualTo(order.getId());
    assertThat(response.finalAmount()).isEqualTo(58000L);
    assertThat(response.tossClientKey()).isEqualTo("test_ck");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "서울 강남구 테헤란로 1",
        " 서울특별시 마포구 1",
        "Seoul, Mapo-gu, World Cup buk-ro 1",
        "  seoul jung-gu sejong-daero 110"
      })
  @DisplayName("주소가 서울·서울특별시·Seoul(대소문자 무시)로 시작하면 주문을 생성한다")
  void create_서울주소_생성성공(String addressLine1) {
    given(productService.getOrderSnapshots(List.of(DOLL.id()))).willReturn(List.of(DOLL));
    given(orderRepository.save(any(Order.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    orderService.create(
        USER_ID, request(addressLine1, List.of(new OrderItemRequest(DOLL.id(), 1))));

    verify(orderRepository).save(any(Order.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {"경기도 성남시 분당구 1", "부산광역시 해운대구 1", "Busan, Haeundae-gu, Haeundae-ro 1"})
  @DisplayName("주소가 서울로 시작하지 않으면 ORDER_ADDRESS_NOT_SEOUL 예외가 발생한다")
  void create_서울외주소_예외발생(String addressLine1) {
    assertThatThrownBy(
            () ->
                orderService.create(
                    USER_ID, request(addressLine1, List.of(new OrderItemRequest(DOLL.id(), 1)))))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.ORDER_ADDRESS_NOT_SEOUL);
    verify(orderRepository, never()).save(any());
  }

  @Test
  @DisplayName("다이어리 상품 수량이 1 을 넘으면 ORDER_INVALID_ITEM 예외가 발생한다")
  void create_다이어리수량초과_예외발생() {
    given(productService.getOrderSnapshots(List.of(DIARY.id()))).willReturn(List.of(DIARY));

    assertThatThrownBy(
            () ->
                orderService.create(
                    USER_ID, request("서울 중구 1", List.of(new OrderItemRequest(DIARY.id(), 2)))))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.ORDER_INVALID_ITEM);
    verify(orderRepository, never()).save(any());
  }

  @Test
  @DisplayName("같은 상품을 여러 줄로 주문하면 ORDER_INVALID_ITEM 예외가 발생한다")
  void create_중복상품_예외발생() {
    assertThatThrownBy(
            () ->
                orderService.create(
                    USER_ID,
                    request(
                        "서울 중구 1",
                        List.of(
                            new OrderItemRequest(DIARY.id(), 1),
                            new OrderItemRequest(DIARY.id(), 1)))))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.ORDER_INVALID_ITEM);
    verify(productService, never()).getOrderSnapshots(any());
  }

  @Test
  @DisplayName("존재하지 않는 상품이 있으면 ORDER_INVALID_ITEM 예외가 발생한다")
  void create_없는상품_예외발생() {
    UUID missingId = UUID.randomUUID();
    given(productService.getOrderSnapshots(List.of(DOLL.id(), missingId)))
        .willReturn(List.of(DOLL));

    assertThatThrownBy(
            () ->
                orderService.create(
                    USER_ID,
                    request(
                        "서울 중구 1",
                        List.of(
                            new OrderItemRequest(DOLL.id(), 1),
                            new OrderItemRequest(missingId, 1)))))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.ORDER_INVALID_ITEM);
    verify(orderRepository, never()).save(any());
  }
}
