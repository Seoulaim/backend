package com.seoulaim.domain.survey.admin.dto;

import java.util.List;

/** questions 는 숨긴 질문 포함 (예전 응답 표시용). responses 는 최신순. */
public record AdminSurveyResultsResponse(
    List<AdminSurveyQuestionResponse> questions,
    List<AdminSurveyAnswerResponse> responses,
    AdminSurveyPopupStats popup) {}
