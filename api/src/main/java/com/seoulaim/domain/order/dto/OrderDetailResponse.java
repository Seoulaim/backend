package com.seoulaim.domain.order.dto;

import com.seoulaim.domain.order.entity.ContactType;
import com.seoulaim.domain.order.entity.Order;
import com.seoulaim.domain.order.entity.OrderItem;
import com.seoulaim.domain.order.entity.OrderStatus;
import com.seoulaim.domain.order.entity.PaymentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(
    UUID orderId,
    String orderNumber,
    OrderStatus status,
    long totalProductAmount,
    long shippingFee,
    long finalAmount,
    Instant orderedAt,
    List<Item> items,
    Shipping shipping,
    PaymentStatus paymentStatus) {

  public static OrderDetailResponse of(
      Order order, List<OrderItem> items, PaymentStatus paymentStatus) {
    return new OrderDetailResponse(
        order.getId(),
        order.getOrderNumber(),
        order.getStatus(),
        order.getTotalProductAmount(),
        order.getShippingFee(),
        order.getFinalAmount(),
        order.getOrderedAt(),
        items.stream().map(Item::from).toList(),
        Shipping.from(order),
        paymentStatus);
  }

  public record Item(UUID productId, String productName, long unitPrice, int quantity) {

    static Item from(OrderItem item) {
      return new Item(
          item.getProductId(), item.getProductName(), item.getUnitPrice(), item.getQuantity());
    }
  }

  public record Shipping(
      String recipientName,
      String addressLine1,
      String addressLine2,
      String postalCode,
      ContactType contactType,
      String contactValue) {

    static Shipping from(Order order) {
      return new Shipping(
          order.getRecipientName(),
          order.getAddressLine1(),
          order.getAddressLine2(),
          order.getPostalCode(),
          order.getContactType(),
          order.getContactValue());
    }
  }
}
