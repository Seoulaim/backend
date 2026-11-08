package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryDownload;
import com.seoulaim.domain.diary.entity.DiaryDownloadFormat;
import com.seoulaim.domain.diary.entity.DiaryPayment;
import com.seoulaim.domain.diary.entity.DiaryPaymentMethod;
import com.seoulaim.domain.diary.entity.DiaryPaymentStatus;
import com.seoulaim.domain.order.gateway.PaymentCurrency;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 환불 관리(어드민)·환불 메일용 결제 정보. API 응답으로 직접 쓰지 않는다.
 *
 * @param amount 결제 통화 단위 금액 (KRW 3000, USD 2.99)
 * @param diary 다이어리를 찾을 수 없으면 null (soft delete 된 다이어리는 그대로 보여준다)
 * @param policy 지금 시점의 환불 정책 항목별 판정
 */
public record PaymentRefundInfo(
    UUID id,
    String orderNumber,
    BigDecimal amount,
    DiaryPaymentMethod method,
    PaymentCurrency currency,
    DiaryPaymentStatus status,
    Instant paidAt,
    Instant refundConsentAt,
    Instant editedAt,
    Instant refundedAt,
    DiaryInfo diary,
    RefundPolicyCheck policy) {

  /**
   * @param first 회원의 첫(무료) 다이어리 (is_free)
   * @param pages 편집 중이면 spreads 수, 완성 후에는 완성 페이지 수
   * @param pageKeys 완성 페이지 key. 없으면 빈 배열
   * @param downloads 최신순
   */
  public record DiaryInfo(
      UUID id,
      String name,
      boolean first,
      int pages,
      Instant completedAt,
      boolean deleted,
      Instant createdAt,
      int saveCount,
      Instant lastSavedAt,
      int photoCount,
      List<String> pageKeys,
      String thumbnailKey,
      List<DownloadInfo> downloads) {

    static DiaryInfo of(Diary diary, List<DiaryDownload> downloads) {
      return new DiaryInfo(
          diary.getId(),
          diary.getName(),
          diary.isFree(),
          diary.pageCount(),
          diary.getCompletedAt(),
          diary.isDeleted(),
          diary.getCreatedAt(),
          diary.getSaveCount(),
          diary.getLastSavedAt(),
          diary.getPhotoCount(),
          diary.getPageKeys() == null ? List.of() : List.copyOf(diary.getPageKeys()),
          diary.thumbnailKey(),
          downloads.stream().map(DownloadInfo::from).toList());
    }
  }

  /**
   * @param at 다운로드 시각
   */
  public record DownloadInfo(DiaryDownloadFormat format, int pageCount, Instant at) {

    static DownloadInfo from(DiaryDownload download) {
      return new DownloadInfo(
          download.getFormat(), download.getPageCount(), download.getCreatedAt());
    }
  }

  /**
   * @param diary 없으면 null
   * @param downloads 이 다이어리의 다운로드 이력 (최신순)
   */
  public static PaymentRefundInfo of(
      DiaryPayment payment, Diary diary, List<DiaryDownload> downloads, Instant now) {
    return new PaymentRefundInfo(
        payment.getId(),
        payment.getOrderNumber(),
        payment.getAmount(),
        payment.getMethod(),
        payment.getCurrency(),
        payment.getStatus(),
        payment.getPaidAt(),
        payment.getRefundConsentAt(),
        payment.getEditedAt(),
        payment.getRefundedAt(),
        diary == null ? null : DiaryInfo.of(diary, downloads),
        RefundPolicyCheck.of(payment, diary, downloads, now));
  }
}
