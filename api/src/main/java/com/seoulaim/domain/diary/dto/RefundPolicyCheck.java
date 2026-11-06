package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryDownload;
import com.seoulaim.domain.diary.entity.DiaryPayment;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 환불 정책 항목별 판정 (어드민 환불 심사용). 판정 시점은 호출 시각(now)이다.
 *
 * <p>환불 문의의 refundWithinPolicy 는 신청 시점 판정이라 이 값과 다를 수 있다 (신청 후 편집·완성, 7일 경과 등). 어드민은 둘 다 본다.
 *
 * <ul>
 *   <li>within7Days: 결제 후 7일 이내 (제3조 1·5항). paidAt + 7일, {@link DiaryPayment#REFUND_PERIOD}
 *   <li>notEdited: 결제 후 편집 이력(edited_at) 없음 (제3조 1·2항)
 *   <li>notFinalized: 다이어리 미완성 (제4조)
 *   <li>notDownloaded: 완성본 다운로드 이력 없음 (제4조. 다운로드는 완성 후에만 가능해 notFinalized 가 참이면 대개 참)
 * </ul>
 *
 * @param allOk 네 항목이 모두 ok
 */
public record RefundPolicyCheck(
    Within7Days within7Days,
    NotEdited notEdited,
    NotFinalized notFinalized,
    NotDownloaded notDownloaded,
    boolean allOk) {

  /**
   * @param deadlineAt paidAt + 7일. 결제 전이면 null
   */
  public record Within7Days(boolean ok, Instant paidAt, Instant deadlineAt) {}

  public record NotEdited(boolean ok, Instant editedAt) {}

  public record NotFinalized(boolean ok, Instant finalizedAt) {}

  public record NotDownloaded(boolean ok, int downloadCount, Instant lastDownloadedAt) {}

  /**
   * @param diary 찾을 수 없으면 null (미완성·다운로드 없음으로 본다)
   * @param downloads 이 다이어리의 다운로드 이력
   */
  public static RefundPolicyCheck of(
      DiaryPayment payment, Diary diary, List<DiaryDownload> downloads, Instant now) {
    Instant paidAt = payment.getPaidAt();
    Instant deadlineAt = paidAt == null ? null : paidAt.plus(DiaryPayment.REFUND_PERIOD);
    Within7Days within7Days =
        new Within7Days(deadlineAt != null && !now.isAfter(deadlineAt), paidAt, deadlineAt);
    NotEdited notEdited = new NotEdited(payment.getEditedAt() == null, payment.getEditedAt());
    boolean finalized = diary != null && diary.isFinalized();
    NotFinalized notFinalized =
        new NotFinalized(!finalized, diary == null ? null : diary.getCompletedAt());
    Instant lastDownloadedAt =
        downloads.stream()
            .map(DiaryDownload::getCreatedAt)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);
    NotDownloaded notDownloaded =
        new NotDownloaded(downloads.isEmpty(), downloads.size(), lastDownloadedAt);
    return new RefundPolicyCheck(
        within7Days,
        notEdited,
        notFinalized,
        notDownloaded,
        within7Days.ok() && notEdited.ok() && notFinalized.ok() && notDownloaded.ok());
  }
}
