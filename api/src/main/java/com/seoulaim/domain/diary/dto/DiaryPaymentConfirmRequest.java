package com.seoulaim.domain.diary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * @param amount 토스 리다이렉트로 받은 결제 통화 단위 금액 (KRW 3000, USD 2.99). 숫자·숫자 문자열 모두 받는다
 */
public record DiaryPaymentConfirmRequest(
    @NotBlank @Size(max = 200) String paymentKey, @NotNull BigDecimal amount) {}
