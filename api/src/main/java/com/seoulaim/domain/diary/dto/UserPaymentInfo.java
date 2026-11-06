package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.diary.entity.DiaryPayment;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * 어드민 회원 상세의 결제 1건 (PAID·REFUNDED). API 응답으로 직접 쓰지 않는다.
 *
 * @param status 소문자 (paid | refunded)
 */
public record UserPaymentInfo(
    UUID id,
    String orderNumber,
    UUID diaryId,
    long amount,
    String status,
    Instant paidAt,
    Instant refundedAt) {

  public static UserPaymentInfo from(DiaryPayment payment) {
    return new UserPaymentInfo(
        payment.getId(),
        payment.getOrderNumber(),
        payment.getDiaryId(),
        payment.getAmount(),
        payment.getStatus().name().toLowerCase(Locale.ROOT),
        payment.getPaidAt(),
        payment.getRefundedAt());
  }
}
