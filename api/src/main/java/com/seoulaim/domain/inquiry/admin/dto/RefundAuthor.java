package com.seoulaim.domain.inquiry.admin.dto;

import com.seoulaim.domain.diary.dto.UserDiaryStats;
import com.seoulaim.domain.inquiry.dto.UserInquiryStats;
import com.seoulaim.domain.user.dto.UserAccountInfo;
import java.time.Instant;
import java.util.UUID;

/**
 * 환불 요청 작성자와 계정 이력 (환불 심사용). 탈퇴 회원은 email 이 null.
 *
 * @param language 회원의 현재 앱 언어 (users.locale). 문의 언어는 상위 language
 * @param diaryCount 삭제되지 않았고 무료이거나 결제된 다이어리 수
 * @param paidPaymentCount 지금 PAID 인 결제 수 (환불된 결제 제외)
 * @param refundedCount 승인된 환불 수
 * @param rejectedRefundCount 거절된 환불 수
 * @param pendingInquiryCount 답변 전 문의 수 (처리 대기 환불 요청 포함)
 */
public record RefundAuthor(
    UUID id,
    String nickname,
    String email,
    Instant joinedAt,
    String language,
    long diaryCount,
    long paidPaymentCount,
    long refundedCount,
    long rejectedRefundCount,
    long pendingInquiryCount,
    boolean withdrawn) {

  /** account 가 null 이면 (조회 불가) id 와 이력 수만 채운다. */
  public static RefundAuthor of(
      UUID userId, UserAccountInfo account, UserDiaryStats diaries, UserInquiryStats inquiries) {
    return new RefundAuthor(
        userId,
        account == null ? null : account.nickname(),
        account == null ? null : account.email(),
        account == null ? null : account.joinedAt(),
        account == null ? null : account.locale(),
        diaries.diaryCount(),
        diaries.paidPaymentCount(),
        inquiries.refundedCount(),
        inquiries.rejectedRefundCount(),
        inquiries.pendingInquiryCount(),
        account != null && account.withdrawn());
  }
}
