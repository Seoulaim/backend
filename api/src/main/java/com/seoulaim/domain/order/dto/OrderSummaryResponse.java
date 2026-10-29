package com.seoulaim.domain.order.dto;

import com.seoulaim.domain.order.entity.Order;
import com.seoulaim.domain.order.entity.OrderItem;
import com.seoulaim.domain.order.entity.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** itemCount 는 주문 상품 행(종류) 수다. 수량 합이 아니다. */
public record OrderSummaryResponse(
    UUID orderId,
    String orderNumber,
    OrderStatus status,
    long finalAmount,
    Instant orderedAt,
    int itemCount,
    String firstProductName) {

  public static OrderSummaryResponse from(Order order, List<OrderItem> items) {
    return new OrderSummaryResponse(
        order.getId(),
        order.getOrderNumber(),
        order.getStatus(),
        order.getFinalAmount(),
        order.getOrderedAt(),
        items.size(),
        items.isEmpty() ? null : items.getFirst().getProductName());
  }
}
