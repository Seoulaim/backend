package com.seoulaim.domain.user.admin.dto;

import com.seoulaim.domain.artist.dto.ArtistWithGroupResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 어드민 회원 상세. 이메일은 마스킹하지 않는다.
 *
 * @param email 탈퇴 회원은 null (탈퇴 시 삭제)
 * @param status 소문자 (active | suspended | withdrawn)
 * @param withdrawnBy 소문자 (admin | self). 탈퇴 전이면 null
 * @param withdrawReason 어드민 강퇴 사유
 * @param marketingConsent 마케팅 수신 동의 (users.marketing_enabled)
 * @param marketingUpdatedAt 마케팅 동의를 마지막으로 바꾼 시각. 가입 후 안 바꿨으면 null
 * @param pushEnabled 푸시 수신 설정
 * @param biases 현재 최애 멤버와 그룹 (탈퇴하면 삭제되어 [])
 * @param diaries 삭제·미결제 포함 최신순
 * @param payments paid·refunded 결제 최신순
 * @param inquiries 환불 포함 모든 문의 최신순
 */
public record AdminUserDetailResponse(
    UUID id,
    String nickname,
    String email,
    String profileImageKey,
    String language,
    Instant joinedAt,
    String status,
    Instant withdrawnAt,
    String withdrawnBy,
    String withdrawReason,
    boolean marketingConsent,
    Instant marketingUpdatedAt,
    boolean pushEnabled,
    List<ArtistWithGroupResponse> biases,
    List<Diary> diaries,
    List<Payment> payments,
    List<Inquiry> inquiries,
    Stats stats) {

  /**
   * @param finalized 완성 여부
   * @param deleted soft delete 됨
   */
  public record Diary(
      UUID id,
      String name,
      boolean free,
      boolean paid,
      boolean finalized,
      Instant createdAt,
      Instant lastSavedAt,
      int saveCount,
      Instant finalizedAt,
      long downloadCount,
      boolean deleted) {}

  /**
   * @param amount 결제 통화 단위 금액 (KRW 3000, USD 2.99)
   * @param method CARD | PAYPAL
   * @param currency KRW | USD
   * @param status 소문자 (paid | refunded)
   */
  public record Payment(
      UUID id,
      String orderNumber,
      UUID diaryId,
      BigDecimal amount,
      String method,
      String currency,
      String status,
      Instant paidAt,
      Instant refundedAt) {}

  /**
   * @param category 소문자
   * @param status 소문자 (received | answered)
   * @param refundStatus 환불 문의만 (requested | refunded | rejected), 그 외 null
   */
  public record Inquiry(
      UUID id,
      String category,
      String title,
      String status,
      Instant createdAt,
      String refundStatus) {}

  /**
   * @param refundedCount 승인된 환불 수
   * @param rejectedRefundCount 거절된 환불 수
   * @param pendingInquiryCount 답변 전 문의 수
   */
  public record Stats(long refundedCount, long rejectedRefundCount, long pendingInquiryCount) {}
}
