package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.diary.entity.DiaryPaymentMethod;
import com.seoulaim.domain.order.gateway.PaymentCurrency;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 어드민 대시보드: KST 날짜 × 통화 × 결제수단별 결제(또는 환불) 건수와 금액.
 *
 * @param amount 통화 단위 금액 합계 (KRW 원, USD 달러)
 */
public record PaymentDailyStat(
    LocalDate date,
    PaymentCurrency currency,
    DiaryPaymentMethod method,
    long count,
    BigDecimal amount) {}
