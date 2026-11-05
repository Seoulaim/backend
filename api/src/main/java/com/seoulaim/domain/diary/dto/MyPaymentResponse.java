package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryPayment;
import com.seoulaim.domain.diary.entity.DiaryPaymentStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * 내 결제 내역 1건.
 *
 * @param name 삭제된 다이어리면 null
 * @param thumbnailKey 삭제된 다이어리이거나 완성 페이지가 없으면 null
 * @param refundable PAID, 결제 후 7일 이내, 다이어리가 FINALIZED 아님, 처리 대기 환불 문의 없음
 * @param withinPolicy refundable 이고 결제 후 편집 이력 없음
 * @param refundRequested 처리 대기(REQUESTED) 환불 문의가 있음
 */
public record MyPaymentResponse(
    UUID id,
    UUID diaryId,
    String name,
    String thumbnailKey,
    long amount,
    DiaryPaymentStatus status,
    Instant paidAt,
    boolean refundable,
    boolean withinPolicy,
    boolean refundRequested) {

  /** diary 가 null 이면 (조회 불가) 삭제된 다이어리처럼 취급한다. */
  public static MyPaymentResponse of(
      DiaryPayment payment, Diary diary, Instant now, boolean refundRequested) {
    boolean visible = diary != null && !diary.isDeleted();
    boolean finalized = diary != null && diary.isFinalized();
    boolean refundable = !refundRequested && payment.isRefundable(now, finalized);
    return new MyPaymentResponse(
        payment.getId(),
        payment.getDiaryId(),
        visible ? diary.getName() : null,
        visible ? diary.thumbnailKey() : null,
        payment.getAmount(),
        payment.getStatus(),
        payment.getPaidAt(),
        refundable,
        refundable && payment.getEditedAt() == null,
        refundRequested);
  }
}
