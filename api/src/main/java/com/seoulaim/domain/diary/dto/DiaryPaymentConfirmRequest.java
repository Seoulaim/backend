package com.seoulaim.domain.diary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DiaryPaymentConfirmRequest(
    @NotBlank @Size(max = 200) String paymentKey, @NotNull Long amount) {}
