package com.seoulaim.domain.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefundDeadlineTest {

  @Test
  @DisplayName("처리 기한은 신청일(KST) + 영업일 3일, 주말은 건너뛴다")
  void of_영업일3일() {
    // 2026-11-05(목) 12:00 KST → 금·월·화
    assertThat(RefundDeadline.of(Instant.parse("2026-11-05T03:00:00Z")))
        .isEqualTo(LocalDate.of(2026, 11, 10));
    // 2026-11-02(월) 23:30 KST (UTC 로는 같은 날 14:30) → 화·수·목
    assertThat(RefundDeadline.of(Instant.parse("2026-11-02T14:30:00Z")))
        .isEqualTo(LocalDate.of(2026, 11, 5));
    // UTC 11-06(금) 16:00 = KST 11-07(토) 01:00 → 월·화·수
    assertThat(RefundDeadline.of(Instant.parse("2026-11-06T16:00:00Z")))
        .isEqualTo(LocalDate.of(2026, 11, 11));
  }
}
