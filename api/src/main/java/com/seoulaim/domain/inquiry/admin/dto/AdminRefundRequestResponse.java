package com.seoulaim.domain.inquiry.admin.dto;

import com.seoulaim.domain.diary.dto.PaymentRefundInfo;
import com.seoulaim.domain.diary.dto.RefundPolicyCheck;
import com.seoulaim.domain.inquiry.entity.Inquiry;
import com.seoulaim.domain.inquiry.entity.RefundStatus;
import java.math.BigDecimal;
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
 * @param policy 지금 시점의 환불 정책 항목별 판정. refundWithinPolicy(신청 시점)와 다를 수 있다. 결제를 찾을 수 없으면 null
 */
public record AdminRefundRequestResponse(
    UUID id,
    RefundAuthor author,
    String language,
    String title,
    String content,
    List<String> photoKeys,
    Instant createdAt,
    Refund refund,
    Payment payment,
    boolean refundWithinPolicy,
    LocalDate deadline,
    RefundPolicyCheck policy) {

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
   * @param amount 결제 통화 단위 금액 (KRW 3000, USD 2.99)
   * @param method CARD | PAYPAL
   * @param currency KRW | USD
   * @param diary 다이어리를 찾을 수 없으면 null
   */
  public record Payment(
      UUID id,
      String orderId,
      BigDecimal amount,
      String method,
      String currency,
      String provider,
      Instant paidAt,
      Instant refundConsentAt,
      Instant editedAt,
      Diary diary) {}

  /**
   * @param first 회원의 첫(무료) 다이어리
   * @param saveCount 편집 데이터 저장 횟수
   * @param photoCount 마지막 저장 시점의 등록 사진 수
   * @param pageKeys 완성 페이지 key. 없으면 []
   * @param thumbnailKey 완성 페이지 첫 장. 없으면 null
   * @param downloads 다운로드 이력 최신순
   * @param deleted soft delete 됨 (환불·탈퇴·회원 삭제)
   */
  public record Diary(
      UUID id,
      String name,
      boolean first,
      int pages,
      Instant completedAt,
      Instant createdAt,
      int saveCount,
      Instant lastSavedAt,
      int photoCount,
      List<String> pageKeys,
      String thumbnailKey,
      List<Download> downloads,
      boolean deleted) {}

  /**
   * @param format PDF | JPG
   * @param at 다운로드 시각
   */
  public record Download(String format, int pageCount, Instant at) {}

  static final String PROVIDER = "toss";

  public static AdminRefundRequestResponse of(
      Inquiry inquiry,
      RefundAuthor author,
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
        deadline,
        payment == null ? null : payment.policy());
  }

  private static Payment toPayment(PaymentRefundInfo payment) {
    PaymentRefundInfo.DiaryInfo diary = payment.diary();
    return new Payment(
        payment.id(),
        payment.orderNumber(),
        payment.amount(),
        payment.method().name(),
        payment.currency().name(),
        PROVIDER,
        payment.paidAt(),
        payment.refundConsentAt(),
        payment.editedAt(),
        diary == null ? null : toDiary(diary));
  }

  private static Diary toDiary(PaymentRefundInfo.DiaryInfo diary) {
    return new Diary(
        diary.id(),
        diary.name(),
        diary.first(),
        diary.pages(),
        diary.completedAt(),
        diary.createdAt(),
        diary.saveCount(),
        diary.lastSavedAt(),
        diary.photoCount(),
        diary.pageKeys(),
        diary.thumbnailKey(),
        diary.downloads().stream()
            .map(
                download ->
                    new Download(download.format().name(), download.pageCount(), download.at()))
            .toList(),
        diary.deleted());
  }
}
