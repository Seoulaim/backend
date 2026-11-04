package com.seoulaim.domain.survey.admin.dto;

import com.seoulaim.domain.survey.dto.SurveyOptionResponse;
import com.seoulaim.domain.survey.entity.SurveyOption;
import com.seoulaim.domain.survey.entity.SurveyQuestion;
import com.seoulaim.domain.survey.entity.SurveyQuestionType;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AdminSurveyQuestionResponse(
    UUID id,
    SurveyQuestionType type,
    Map<String, String> title,
    boolean required,
    boolean archived,
    List<SurveyOptionResponse> options) {

  public static AdminSurveyQuestionResponse of(
      SurveyQuestion question, List<SurveyOption> options) {
    return new AdminSurveyQuestionResponse(
        question.getId(),
        question.getType(),
        question.getTitle(),
        question.isRequired(),
        question.isArchived(),
        options.stream().map(SurveyOptionResponse::from).toList());
  }
}
