package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.order.dto.OrderItemInfo;
import java.time.Instant;
import java.util.UUID;

public record DiaryPendingOrderResponse(
    UUID orderItemId, UUID orderId, String orderNumber, String productName, Instant paidAt) {

  public static DiaryPendingOrderResponse from(OrderItemInfo item) {
    return new DiaryPendingOrderResponse(
        item.orderItemId(), item.orderId(), item.orderNumber(), item.productName(), item.paidAt());
  }
}
