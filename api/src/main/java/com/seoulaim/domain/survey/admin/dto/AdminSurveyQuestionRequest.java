package com.seoulaim.domain.survey.admin.dto;

import com.seoulaim.domain.survey.entity.SurveyQuestionType;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** id 가 없으면 새 질문. type 은 대소문자 구분 없음. options 는 SINGLE / MULTIPLE 만 쓰고 나머지 type 에서는 무시한다. */
public record AdminSurveyQuestionRequest(
    UUID id,
    SurveyQuestionType type,
    Map<String, String> title,
    boolean required,
    boolean archived,
    List<AdminSurveyOptionRequest> options) {

  public List<AdminSurveyOptionRequest> optionsOrEmpty() {
    return options == null ? List.of() : options;
  }
}
