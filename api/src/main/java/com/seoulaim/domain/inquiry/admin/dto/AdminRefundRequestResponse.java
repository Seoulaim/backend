package com.seoulaim.domain.inquiry.admin.dto;

import com.seoulaim.domain.diary.dto.PaymentRefundInfo;
import com.seoulaim.domain.inquiry.entity.Inquiry;
import com.seoulaim.domain.inquiry.entity.RefundStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 환불 관리 1건 (= 환불 문의).
 *
 * @param createdAt 환불을 요청한 시각
 * @param refundWithinPolicy 신청 시점의 서버 판정. false 면 승인에 예외 사유가 필요하다
 * @param deadline 처리 기한: 신청일(KST) + 영업일 3일 (주말 제외, 공휴일 미반영)
 */
public record AdminRefundRequestResponse(
    UUID id,
    InquiryAuthor author,
    String language,
    String title,
    String content,
    List<String> photoKeys,
    Instant createdAt,
    Refund refund,
    Payment payment,
    boolean refundWithinPolicy,
    LocalDate deadline) {

  /**
   * @param status 소문자 (requested | refunded | rejected)
   * @param adminName 결정 전이면 null
   * @param exception 정책 밖 승인 사유. 없으면 null
   */
  public record Refund(
      RefundStatus status,
      Instant decidedAt,
      String adminName,
      RefundException exception,
      String rejectReason) {}

  public record RefundException(String reason, String memo) {}

  /**
   * @param orderId 토스 orderId (= diary_payments.order_number)
   * @param diary 다이어리를 찾을 수 없으면 null
   */
  public record Payment(
      UUID id,
      String orderId,
      long amount,
      String provider,
      Instant paidAt,
      Instant refundConsentAt,
      Instant editedAt,
      Diary diary) {}

  /**
   * @param first 회원의 첫(무료) 다이어리
   */
  public record Diary(UUID id, String name, boolean first, int pages, Instant completedAt) {}

  static final String PROVIDER = "toss";

  public static AdminRefundRequestResponse of(
      Inquiry inquiry,
      InquiryAuthor author,
      PaymentRefundInfo payment,
      String adminName,
      LocalDate deadline) {
    boolean decided = !inquiry.isRefundRequested();
    Refund refund =
        new Refund(
            inquiry.getRefundStatus(),
            inquiry.getRefundDecidedAt(),
            decided ? adminName : null,
            inquiry.getRefundExceptionReason() == null
                ? null
                : new RefundException(
                    inquiry.getRefundExceptionReason(), inquiry.getRefundExceptionMemo()),
            inquiry.getRefundRejectReason());
    return new AdminRefundRequestResponse(
        inquiry.getId(),
        author,
        inquiry.getLanguage(),
        inquiry.getTitle(),
        inquiry.getContent(),
        inquiry.getPhotoKeys(),
        inquiry.getCreatedAt(),
        refund,
        payment == null ? null : toPayment(payment),
        inquiry.isRefundWithinPolicy(),
        deadline);
  }

  private static Payment toPayment(PaymentRefundInfo payment) {
    PaymentRefundInfo.DiaryInfo diary = payment.diary();
    return new Payment(
        payment.id(),
        payment.orderNumber(),
        payment.amount(),
        PROVIDER,
        payment.paidAt(),
        payment.refundConsentAt(),
        payment.editedAt(),
        diary == null
            ? null
            : new Diary(
                diary.id(), diary.name(), diary.first(), diary.pages(), diary.completedAt()));
  }
}
