package com.seoulaim.domain.survey.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

/**
 * 초대 팝업 이벤트. SHOWN(노출) · ACCEPTED(예) · CLOSED(닫기) · SNOOZED(오늘 하루 보지 않기).
 *
 * <p>앱은 소문자(shown 등)를 보내므로 대소문자 구분 없이 받는다. 모르는 값은 null → 400.
 */
public enum SurveyEventType {
  SHOWN,
  ACCEPTED,
  CLOSED,
  SNOOZED;

  @JsonCreator
  public static SurveyEventType from(String value) {
    if (value == null) {
      return null;
    }
    try {
      return valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }
}
