package com.seoulaim.domain.diary.admin.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * 어드민 결제 관리 1건. 필드명은 어드민 mock(api/payments.js) 기준.
 *
 * @param orderId 토스 orderId (= diary_payments.order_number). 토스 콘솔과 대조할 때 쓴다
 * @param member 결제한 회원. 탈퇴 회원은 email null
 * @param diary 다이어리를 찾을 수 없으면 null
 * @param provider "toss"
 * @param status 소문자 (pending | paid | failed | refunded)
 * @param editedAt 결제 후 첫 편집 시각 (환불 정책 판단용)
 * @param refundRequested 처리 대기(requested) 환불 요청이 있음
 * @param refundRequestId 가장 최근 환불 요청(문의) id. 없으면 null
 * @param method 토스 승인 응답의 결제수단. 저장 전 결제(#57 이전)나 미승인 결제는 null
 * @param sameDiaryPayments 이 결제가 paid 일 때 같은 다이어리의 다른 paid 결제 수 (1 이상이면 중복 결제 의심)
 */
public record AdminPaymentResponse(
    UUID id,
    String orderId,
    Member member,
    Diary diary,
    long amount,
    String provider,
    String status,
    Instant createdAt,
    Instant paidAt,
    Instant refundedAt,
    Instant refundConsentAt,
    Instant editedAt,
    boolean refundRequested,
    UUID refundRequestId,
    String method,
    int sameDiaryPayments) {

  public record Member(UUID id, String nickname, String email) {}

  /**
   * @param first 회원의 첫(무료) 다이어리
   * @param pages 편집 중이면 spreads 수, 완성 후에는 완성 페이지 수
   * @param deleted soft delete 됨
   */
  public record Diary(
      UUID id, String name, boolean first, int pages, Instant completedAt, boolean deleted) {}
}
