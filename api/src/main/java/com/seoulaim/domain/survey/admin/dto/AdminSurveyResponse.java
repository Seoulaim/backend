package com.seoulaim.domain.survey.admin.dto;

import com.seoulaim.domain.survey.dto.SurveyInvite;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 어드민 설문 설정.
 *
 * @param updatedAt 설문(문구·질문)을 마지막으로 저장하거나 켜고 끈 시각
 * @param answerCounts 질문별 응답 수 { [questionId]: n } (응답 없는 질문은 0)
 */
public record AdminSurveyResponse(
    boolean enabled,
    SurveyInvite invite,
    List<AdminSurveyQuestionResponse> questions,
    Instant updatedAt,
    Map<String, Long> answerCounts) {}
