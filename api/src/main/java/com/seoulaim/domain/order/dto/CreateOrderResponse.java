package com.seoulaim.domain.order.dto;

import java.util.UUID;

public record CreateOrderResponse(
    UUID orderId, String orderNumber, long finalAmount, String tossClientKey) {}
