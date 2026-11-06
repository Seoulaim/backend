package com.seoulaim.domain.diary.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryDownload;
import com.seoulaim.domain.diary.entity.DiaryDownloadFormat;
import com.seoulaim.domain.diary.entity.DiaryPayment;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RefundPolicyCheckTest {

  private static final Instant PAID_AT = Instant.parse("2026-11-01T03:00:00Z");
  private static final UUID USER_ID = UUID.randomUUID();

  private static Diary diary() {
    return Diary.create(USER_ID, false);
  }

  private static DiaryPayment paid(Diary diary) {
    DiaryPayment payment =
        DiaryPayment.createPending(
            diary.getId(), USER_ID, "DIA-20261101-ABCDE12345", 3000, PAID_AT);
    payment.markPaid("pk", PAID_AT);
    return payment;
  }

  private static DiaryDownload download(Diary diary, Instant at) {
    DiaryDownload download =
        DiaryDownload.create(diary.getId(), USER_ID, DiaryDownloadFormat.PDF, 4);
    ReflectionTestUtils.setField(download, "createdAt", at);
    return download;
  }

  @Test
  @DisplayName("결제 후 7일 이내·미편집·미완성·미다운로드면 네 항목 모두 ok 이고 allOk")
  void of_모두정상_allOk() {
    Diary diary = diary();
    Instant now = PAID_AT.plus(Duration.ofDays(3));

    RefundPolicyCheck check = RefundPolicyCheck.of(paid(diary), diary, List.of(), now);

    assertThat(check.within7Days())
        .isEqualTo(
            new RefundPolicyCheck.Within7Days(true, PAID_AT, PAID_AT.plus(Duration.ofDays(7))));
    assertThat(check.notEdited()).isEqualTo(new RefundPolicyCheck.NotEdited(true, null));
    assertThat(check.notFinalized()).isEqualTo(new RefundPolicyCheck.NotFinalized(true, null));
    assertThat(check.notDownloaded()).isEqualTo(new RefundPolicyCheck.NotDownloaded(true, 0, null));
    assertThat(check.allOk()).isTrue();
  }

  @Test
  @DisplayName("within7Days: paidAt + 7일 정각까지 ok, 1초 지나면 false. 결제 전(paidAt null)이면 false")
  void of_7일경계() {
    Diary diary = diary();
    DiaryPayment payment = paid(diary);
    Instant deadline = PAID_AT.plus(Duration.ofDays(7));

    assertThat(RefundPolicyCheck.of(payment, diary, List.of(), deadline).within7Days().ok())
        .isTrue();
    RefundPolicyCheck late =
        RefundPolicyCheck.of(payment, diary, List.of(), deadline.plusSeconds(1));
    assertThat(late.within7Days().ok()).isFalse();
    assertThat(late.allOk()).isFalse();

    DiaryPayment pending =
        DiaryPayment.createPending(
            diary.getId(), USER_ID, "DIA-20261101-PENDING001", 3000, PAID_AT);
    RefundPolicyCheck unpaid = RefundPolicyCheck.of(pending, diary, List.of(), PAID_AT);
    assertThat(unpaid.within7Days())
        .isEqualTo(new RefundPolicyCheck.Within7Days(false, null, null));
  }

  @Test
  @DisplayName("notEdited: 결제 후 편집 기록(edited_at)이 있으면 false 와 편집 시각")
  void of_편집후_notEditedFalse() {
    Diary diary = diary();
    DiaryPayment payment = paid(diary);
    Instant editedAt = PAID_AT.plusSeconds(600);
    payment.recordEdited(editedAt);

    RefundPolicyCheck check =
        RefundPolicyCheck.of(payment, diary, List.of(), PAID_AT.plusSeconds(3600));

    assertThat(check.notEdited()).isEqualTo(new RefundPolicyCheck.NotEdited(false, editedAt));
    assertThat(check.allOk()).isFalse();
  }

  @Test
  @DisplayName("notFinalized: 완성(FINALIZED)된 다이어리면 false 와 완성 시각. 다이어리를 못 찾으면 ok")
  void of_완성후_notFinalizedFalse() {
    Diary diary = diary();
    Instant finalizedAt = PAID_AT.plusSeconds(1200);
    diary.finalizeWith(List.of(), Map.of(), 0, finalizedAt);

    RefundPolicyCheck check =
        RefundPolicyCheck.of(paid(diary), diary, List.of(), PAID_AT.plusSeconds(3600));

    assertThat(check.notFinalized())
        .isEqualTo(new RefundPolicyCheck.NotFinalized(false, finalizedAt));
    assertThat(check.allOk()).isFalse();
    assertThat(RefundPolicyCheck.of(paid(diary), null, List.of(), PAID_AT).notFinalized().ok())
        .isTrue();
  }

  @Test
  @DisplayName("notDownloaded: 다운로드 기록이 있으면 false, 횟수와 마지막 다운로드 시각")
  void of_다운로드후_notDownloadedFalse() {
    Diary diary = diary();
    Instant first = PAID_AT.plusSeconds(2000);
    Instant last = PAID_AT.plusSeconds(4000);

    RefundPolicyCheck check =
        RefundPolicyCheck.of(
            paid(diary),
            diary,
            List.of(download(diary, last), download(diary, first)),
            PAID_AT.plusSeconds(5000));

    assertThat(check.notDownloaded())
        .isEqualTo(new RefundPolicyCheck.NotDownloaded(false, 2, last));
    assertThat(check.allOk()).isFalse();
  }
}
