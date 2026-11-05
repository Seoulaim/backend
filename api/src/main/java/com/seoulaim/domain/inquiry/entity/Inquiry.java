package com.seoulaim.domain.inquiry.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Help Center 문의 (MR03). 유형이 REFUND 인 문의는 회원이 고른 다이어리 결제(paymentId)와 환불 처리 상태를 가진다.
 *
 * <p>다른 도메인 엔티티를 참조하지 않으려고 userId·paymentId 만 보관한다. DB 에는 FK 가 있다.
 */
@Getter
@Entity
@Table(name = "inquiries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseEntity {

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false, length = 20)
  private InquiryCategory category;

  @Column(name = "title", nullable = false, length = 50)
  private String title;

  @Column(name = "content", nullable = false, columnDefinition = "text")
  private String content;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "photo_keys", nullable = false, columnDefinition = "jsonb")
  private List<String> photoKeys;

  // 회원이 쓴 앱 언어 (en|ko|zh|ja). 답변은 이 언어로 한다
  @Column(name = "language", nullable = false, length = 5)
  private String language;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private InquiryStatus status;

  // 어드민 전용 메모
  @Column(name = "memo", columnDefinition = "text")
  private String memo;

  @Column(name = "payment_id")
  private UUID paymentId;

  // 신청 시점의 환불 정책 판정. 이후 편집·경과일과 무관하게 이 값으로 승인 조건을 본다
  @Column(name = "refund_within_policy")
  private Boolean refundWithinPolicy;

  @Enumerated(EnumType.STRING)
  @Column(name = "refund_status", length = 20)
  private RefundStatus refundStatus;

  @Column(name = "refund_decided_at")
  private Instant refundDecidedAt;

  @Column(name = "refund_exception_reason", length = 50)
  private String refundExceptionReason;

  @Column(name = "refund_exception_memo", columnDefinition = "text")
  private String refundExceptionMemo;

  @Column(name = "refund_reject_reason", columnDefinition = "text")
  private String refundRejectReason;

  /** 환불 외 일반 문의. */
  public static Inquiry create(
      UUID userId,
      InquiryCategory category,
      String title,
      String content,
      List<String> photoKeys,
      String language) {
    if (category == InquiryCategory.REFUND) {
      throw new IllegalArgumentException("환불 문의는 createRefund 로 만든다");
    }
    return base(userId, category, title, content, photoKeys, language);
  }

  /** 환불 문의. refundWithinPolicy 는 신청 시점 판정. */
  public static Inquiry createRefund(
      UUID userId,
      String title,
      String content,
      List<String> photoKeys,
      String language,
      UUID paymentId,
      boolean refundWithinPolicy) {
    Inquiry inquiry = base(userId, InquiryCategory.REFUND, title, content, photoKeys, language);
    inquiry.paymentId = paymentId;
    inquiry.refundWithinPolicy = refundWithinPolicy;
    inquiry.refundStatus = RefundStatus.REQUESTED;
    return inquiry;
  }

  private static Inquiry base(
      UUID userId,
      InquiryCategory category,
      String title,
      String content,
      List<String> photoKeys,
      String language) {
    Inquiry inquiry = new Inquiry();
    inquiry.userId = userId;
    inquiry.category = category;
    inquiry.title = title;
    inquiry.content = content;
    inquiry.photoKeys = List.copyOf(photoKeys);
    inquiry.language = language;
    inquiry.status = InquiryStatus.RECEIVED;
    return inquiry;
  }

  /** 답변 메일을 보냈거나 환불을 결정했다. */
  public void markAnswered() {
    this.status = InquiryStatus.ANSWERED;
  }

  public void updateMemo(String memo) {
    this.memo = memo;
  }

  /** 환불 승인. 정책 밖 승인이면 예외 사유를 남긴다 (정책 안이어도 넘어오면 그대로 남긴다). */
  public void approveRefund(String exceptionReason, String exceptionMemo, Instant now) {
    requirePendingRefund();
    this.refundStatus = RefundStatus.REFUNDED;
    this.refundDecidedAt = now;
    this.refundExceptionReason = exceptionReason;
    this.refundExceptionMemo = exceptionMemo;
    markAnswered();
  }

  public void rejectRefund(String reason, Instant now) {
    requirePendingRefund();
    this.refundStatus = RefundStatus.REJECTED;
    this.refundDecidedAt = now;
    this.refundRejectReason = reason;
    markAnswered();
  }

  private void requirePendingRefund() {
    if (!isRefundRequested()) {
      throw new IllegalStateException("처리 대기 중인 환불 문의가 아니다: " + getId());
    }
  }

  public boolean isRefund() {
    return category == InquiryCategory.REFUND;
  }

  public boolean isRefundRequested() {
    return refundStatus == RefundStatus.REQUESTED;
  }

  public boolean isRefundWithinPolicy() {
    return Boolean.TRUE.equals(refundWithinPolicy);
  }
}
