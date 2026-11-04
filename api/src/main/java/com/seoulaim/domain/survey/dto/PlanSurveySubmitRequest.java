package com.seoulaim.domain.survey.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * answers: { [questionId]: 별점 숫자 | 보기 id | 보기 id 배열 | 글 }. 답하지 않은 질문은 빠진다.
 *
 * <p>값 형식은 질문 type 에 따라 달라 서비스에서 검증한다.
 */
public record PlanSurveySubmitRequest(@NotNull Map<String, Object> answers) {}
