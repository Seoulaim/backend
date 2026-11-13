package com.seoulaim.domain.inquiry.admin.dto;

import com.seoulaim.domain.order.gateway.PaymentCurrency;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 대시보드 처리 대기 환불 1건 (어드민 mock dashboard.js pendingRefunds 필드 + currency·deadline).
 *
 * @param id 환불 문의 id
 * @param nickname 작성자 닉네임. 회원을 찾을 수 없으면 null
 * @param amount 결제 통화 단위 금액 (KRW 3000, USD 2.99). 결제를 찾을 수 없으면 null
 * @param currency KRW | USD. 결제를 찾을 수 없으면 null
 * @param deadline 처리 기한 (신청일 + 영업일 3일, KST)
 */
public record AdminPendingRefundResponse(
    UUID id,
    String title,
    String nickname,
    BigDecimal amount,
    PaymentCurrency currency,
    Instant createdAt,
    LocalDate deadline) {}
