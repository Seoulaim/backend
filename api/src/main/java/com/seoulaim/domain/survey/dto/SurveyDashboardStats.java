package com.seoulaim.domain.survey.dto;

/**
 * 어드민 대시보드 설문 지표.
 *
 * @param averageRating 숨기지 않은 첫 별점(RATING) 질문의 전체 기간 평균 (어드민 lib headlineRating 과 같음). 질문·응답이 없으면
 *     null
 * @param responsesThisWeek 최근 7일(7×24시간) 응답 수
 * @param responsesInRange 대시보드 기간 내 응답 수
 */
public record SurveyDashboardStats(
    Double averageRating, long responsesThisWeek, long responsesInRange) {}
