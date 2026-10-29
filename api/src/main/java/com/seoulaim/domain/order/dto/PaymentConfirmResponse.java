package com.seoulaim.domain.order.dto;

import com.seoulaim.domain.order.entity.OrderStatus;
import java.util.UUID;

public record PaymentConfirmResponse(UUID orderId, OrderStatus status) {}
