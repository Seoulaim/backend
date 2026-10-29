package com.seoulaim.domain.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record OrderItemRequest(
    @NotNull UUID productId, @NotNull @Positive @Max(99) Integer quantity) {}
