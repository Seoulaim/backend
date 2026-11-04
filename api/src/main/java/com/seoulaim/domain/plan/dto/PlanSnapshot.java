package com.seoulaim.domain.plan.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 일정 요약 (다른 도메인 제공용). 만족도 조사 응답에 응답 시점 일정으로 저장된다.
 *
 * @param dayCount 투어하는 날 수 (PlanDay 수)
 * @param placeCount 일정에 들어간 서로 다른 장소 수
 */
public record PlanSnapshot(
    UUID planId, LocalDate startDate, LocalDate endDate, int dayCount, int placeCount) {}
