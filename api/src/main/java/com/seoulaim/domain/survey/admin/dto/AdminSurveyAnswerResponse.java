package com.seoulaim.domain.survey.admin.dto;

import com.seoulaim.domain.survey.entity.SurveyGiftStatus;
import com.seoulaim.domain.survey.entity.SurveyResponse;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 회원 한 명의 설문 응답.
 *
 * @param plan 응답 시점 플랜 요약 { planId, startDate, endDate, dayCount, placeCount }
 * @param gift 기프티콘 발송 상태
 */
public record AdminSurveyAnswerResponse(
    UUID id,
    UUID userId,
    String email,
    Instant submittedAt,
    Map<String, Object> answers,
    Map<String, Object> plan,
    Gift gift) {

  public record Gift(SurveyGiftStatus status, Instant sentAt) {}

  public static AdminSurveyAnswerResponse from(SurveyResponse response) {
    return new AdminSurveyAnswerResponse(
        response.getId(),
        response.getUserId(),
        response.getEmail(),
        response.getSubmittedAt(),
        response.getAnswers(),
        response.getPlanSnapshot(),
        new Gift(response.getGiftStatus(), response.getGiftSentAt()));
  }
}
