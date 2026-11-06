package com.seoulaim.domain.diary.admin.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.UUID;

/**
 * 어드민 결제 상세: 목록 항목(펼쳐서) + 토스 키·실패 사유·환불 요청.
 *
 * @param paymentKey 토스 paymentKey. 승인 전이면 null
 * @param tossCancelKey 토스 취소 거래 키. 환불 전이면 null
 * @param refundRequest 가장 최근 환불 요청. 없으면 null
 */
public record AdminPaymentDetailResponse(
    @JsonUnwrapped AdminPaymentResponse payment,
    String paymentKey,
    String tossCancelKey,
    String failureCode,
    String failureMessage,
    RefundRequest refundRequest) {

  /**
   * @param status 소문자 (requested | refunded | rejected)
   */
  public record RefundRequest(UUID inquiryId, String status) {}
}
