package com.seoulaim.domain.order.dto;

import java.time.Instant;
import java.util.UUID;

/** 다른 도메인(diary)이 주문 상품 줄을 조회할 때 쓰는 정보. paidAt 은 결제 승인 전이면 null. */
public record OrderItemInfo(
    UUID orderItemId,
    UUID orderId,
    String orderNumber,
    UUID productId,
    String productName,
    Instant paidAt) {}
