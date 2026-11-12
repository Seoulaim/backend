package com.seoulaim.domain.dashboard.service;

import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.Arrays;
import java.util.Map;

/** 대시보드 기간. 오늘(KST) 포함 N일. */
public enum DashboardRange {
  DAYS_7("7d", 7),
  DAYS_30("30d", 30),
  DAYS_90("90d", 90);

  private final String value;
  private final int days;

  DashboardRange(String value, int days) {
    this.value = value;
    this.days = days;
  }

  public String value() {
    return value;
  }

  public int days() {
    return days;
  }

  /** null·빈 값이면 7d. 대소문자 무시. 없는 값은 COMMON_INVALID_REQUEST(400). */
  public static DashboardRange parse(String value) {
    if (value == null || value.isBlank()) {
      return DAYS_7;
    }
    return Arrays.stream(values())
        .filter(range -> range.value.equalsIgnoreCase(value.strip()))
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST,
                    Map.of("range", "7d | 30d | 90d 중 하나여야 합니다")));
  }
}
