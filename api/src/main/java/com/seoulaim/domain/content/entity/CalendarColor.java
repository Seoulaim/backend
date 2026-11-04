package com.seoulaim.domain.content.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/** 팬캘린더 태그 색. DB 는 대문자, API 는 앱·어드민과 같은 소문자(pink 등)로 주고받는다. */
public enum CalendarColor {
  PINK,
  PURPLE,
  BLUE,
  ORANGE,
  GRAY,
  GREEN;

  @JsonValue
  public String toJson() {
    return name().toLowerCase(Locale.ROOT);
  }

  // 대소문자 무시. 모르는 값이면 400 (fields.color)
  @JsonCreator
  public static CalendarColor from(String value) {
    return Arrays.stream(values())
        .filter(color -> color.name().equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST,
                    Map.of("color", "pink|purple|blue|orange|gray|green 중 하나여야 합니다")));
  }
}
