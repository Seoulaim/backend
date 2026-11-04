package com.seoulaim.domain.survey.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

/**
 * 질문 형식. 앱·어드민 목업은 소문자(rating 등)를 쓰므로 요청은 대소문자 구분 없이 받는다.
 *
 * <p>모르는 값은 null 로 받아 서비스에서 SURVEY_INVALID 로 처리한다.
 */
public enum SurveyQuestionType {
  RATING,
  SINGLE,
  MULTIPLE,
  TEXT;

  @JsonCreator
  public static SurveyQuestionType from(String value) {
    if (value == null) {
      return null;
    }
    try {
      return valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  public boolean hasOptions() {
    return this == SINGLE || this == MULTIPLE;
  }
}
