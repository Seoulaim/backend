package com.seoulaim.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.seoulaim.domain.place.entity.PlaceOpeningHour;
import com.seoulaim.domain.place.entity.Weekday;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OpeningHoursTest {

  // 월 16:00 ~ 다음 날 01:00, 화 휴무
  private final PlaceOpeningHour monday =
      PlaceOpeningHour.open(null, Weekday.MON, LocalTime.of(16, 0), LocalTime.of(1, 0), null, null);
  private final PlaceOpeningHour tuesday = PlaceOpeningHour.closedOn(null, Weekday.TUE);

  @Test
  @DisplayName("자정을 넘기는 영업시간과 그 구간 안의 브레이크는 저장 검증을 통과한다")
  void isValid_자정넘김() {
    assertThat(OpeningHours.isValid(LocalTime.of(16, 0), LocalTime.of(1, 0), null, null)).isTrue();
    // 브레이크가 자정을 걸쳐도 영업 구간 안이면 허용
    assertThat(
            OpeningHours.isValid(
                LocalTime.of(16, 0), LocalTime.of(1, 0), LocalTime.of(23, 30), LocalTime.of(0, 15)))
        .isTrue();
    // 브레이크가 마감 뒤면 거절
    assertThat(
            OpeningHours.isValid(
                LocalTime.of(16, 0), LocalTime.of(1, 0), LocalTime.of(0, 30), LocalTime.of(2, 0)))
        .isFalse();
  }

  @Test
  @DisplayName("전날 16:00 ~ 01:00 영업이면 다음 날 00:30 은 영업 중이다")
  void isOpenNow_전날자정넘김_새벽() {
    assertThat(OpeningHours.isOpenNow(tuesday, monday, LocalTime.of(0, 30))).isTrue();
  }

  @Test
  @DisplayName("전날 16:00 ~ 01:00 영업이면 다음 날 02:00 은 영업 중이 아니다")
  void isOpenNow_전날자정넘김_마감후() {
    assertThat(OpeningHours.isOpenNow(tuesday, monday, LocalTime.of(2, 0))).isFalse();
  }
}
