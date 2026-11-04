package com.seoulaim.domain.place.admin.dto;

import com.seoulaim.domain.place.dto.PlaceOpeningHourResponse;
import com.seoulaim.domain.place.entity.LocalizedText;
import com.seoulaim.domain.place.entity.PlaceCategory;
import java.util.List;
import java.util.UUID;

/**
 * 어드민 장소 목록 항목
 *
 * <ul>
 *   <li>name: 언어별 이름, 빈 언어는 null
 *   <li>thumbnailKey: 대표 사진, 없으면 null
 *   <li>artistCount: 멤버 태그 수
 *   <li>operatingPeriod: 상시 운영이면 null
 *   <li>openingHours: 목록 요약 표시용, 영업시간 없음이면 null
 * </ul>
 */
public record AdminPlaceSummaryResponse(
    UUID id,
    LocalizedText name,
    PlaceCategory category,
    boolean visible,
    String thumbnailKey,
    long artistCount,
    long keywordCount,
    AdminPlaceOperatingPeriod operatingPeriod,
    List<PlaceOpeningHourResponse> openingHours) {}
