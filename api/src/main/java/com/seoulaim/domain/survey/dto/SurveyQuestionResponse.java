package com.seoulaim.domain.survey.dto;

import com.seoulaim.domain.survey.entity.SurveyOption;
import com.seoulaim.domain.survey.entity.SurveyQuestion;
import com.seoulaim.domain.survey.entity.SurveyQuestionType;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 회원 앱용 질문 (숨긴 질문 제외). options 는 SINGLE / MULTIPLE 만, 나머지는 빈 배열. */
public record SurveyQuestionResponse(
    UUID id,
    SurveyQuestionType type,
    Map<String, String> title,
    boolean required,
    List<SurveyOptionResponse> options) {

  public static SurveyQuestionResponse of(SurveyQuestion question, List<SurveyOption> options) {
    return new SurveyQuestionResponse(
        question.getId(),
        question.getType(),
        question.getTitle(),
        question.isRequired(),
        options.stream().map(SurveyOptionResponse::from).toList());
  }
}
