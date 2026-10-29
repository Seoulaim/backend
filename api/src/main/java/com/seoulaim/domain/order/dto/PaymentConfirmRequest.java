package com.seoulaim.domain.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PaymentConfirmRequest(
    @NotBlank @Size(max = 255) String paymentKey, @NotNull @Positive Long amount) {}
