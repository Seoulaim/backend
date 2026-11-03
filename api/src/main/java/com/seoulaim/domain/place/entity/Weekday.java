package com.seoulaim.domain.place.entity;

import java.time.DayOfWeek;

// 영업시간 요일. DB·API 값은 3글자 (MON ~ SUN)
public enum Weekday {
  MON,
  TUE,
  WED,
  THU,
  FRI,
  SAT,
  SUN;

  public static Weekday from(DayOfWeek dayOfWeek) {
    return values()[dayOfWeek.getValue() - 1];
  }
}
