package com.seoulaim.domain.survey.dto;

import java.util.List;

/**
 * 설문 화면 데이터.
 *
 * @param email 기프티콘을 받을 계정 이메일
 * @param answered 이미 응답했는지
 */
public record PlanSurveyResponse(
    SurveyInvite invite, List<SurveyQuestionResponse> questions, String email, boolean answered) {}
