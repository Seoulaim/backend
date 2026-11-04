package com.seoulaim.domain.survey.admin.dto;

/**
 * 초대 팝업 누적 통계 (기간 필터와 무관하게 전체 기간).
 *
 * @param planCreators 플랜이 있는 회원 수
 * @param responded 응답 수
 * @param shown 팝업 노출 수
 * @param closed 닫기(X) 수
 * @param accepted 예 수
 * @param snoozed 오늘 하루 보지 않기 수
 */
public record AdminSurveyPopupStats(
    long planCreators, long responded, long shown, long closed, long accepted, long snoozed) {}
