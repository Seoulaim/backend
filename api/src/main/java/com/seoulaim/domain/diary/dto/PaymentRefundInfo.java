package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryPayment;
import com.seoulaim.domain.diary.entity.DiaryPaymentStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * 환불 관리(어드민)·환불 메일용 결제 정보. API 응답으로 직접 쓰지 않는다.
 *
 * @param diary 다이어리를 찾을 수 없으면 null (soft delete 된 다이어리는 그대로 보여준다)
 */
public record PaymentRefundInfo(
    UUID id,
    String orderNumber,
    long amount,
    DiaryPaymentStatus status,
    Instant paidAt,
    Instant refundConsentAt,
    Instant editedAt,
    Instant refundedAt,
    DiaryInfo diary) {

  /**
   * @param first 회원의 첫(무료) 다이어리 (is_free)
   * @param pages 편집 중이면 spreads 수, 완성 후에는 완성 페이지 수
   */
  public record DiaryInfo(
      UUID id, String name, boolean first, int pages, Instant completedAt, boolean deleted) {

    static DiaryInfo from(Diary diary) {
      return new DiaryInfo(
          diary.getId(),
          diary.getName(),
          diary.isFree(),
          diary.pageCount(),
          diary.getCompletedAt(),
          diary.isDeleted());
    }
  }

  public static PaymentRefundInfo of(DiaryPayment payment, Diary diary) {
    return new PaymentRefundInfo(
        payment.getId(),
        payment.getOrderNumber(),
        payment.getAmount(),
        payment.getStatus(),
        payment.getPaidAt(),
        payment.getRefundConsentAt(),
        payment.getEditedAt(),
        payment.getRefundedAt(),
        diary == null ? null : DiaryInfo.from(diary));
  }
}
