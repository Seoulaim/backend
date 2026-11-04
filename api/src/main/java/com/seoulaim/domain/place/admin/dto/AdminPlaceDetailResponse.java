package com.seoulaim.domain.place.admin.dto;

import com.seoulaim.domain.place.dto.PlaceDetailResponse.ArtistLink;
import com.seoulaim.domain.place.dto.PlaceDetailResponse.Location;
import com.seoulaim.domain.place.dto.PlaceOpeningHourResponse;
import com.seoulaim.domain.place.entity.LocalizedText;
import com.seoulaim.domain.place.entity.PlaceCategory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 어드민 장소 상세 (수정 폼용). 숨김 장소도 조회
 *
 * <ul>
 *   <li>name·summary·address: 언어별 원본, 빈 언어는 null
 *   <li>imageKeys: 첫 번째가 대표 사진
 *   <li>operatingPeriod: 상시 운영이면 null
 *   <li>openingHours: 월 ~ 일 7개, 영업시간 없음이면 null
 *   <li>artists[].artistId·artistNameEn: 그룹 단위 연결이면 null
 * </ul>
 */
public record AdminPlaceDetailResponse(
    UUID id,
    LocalizedText name,
    LocalizedText summary,
    PlaceCategory category,
    boolean visible,
    LocalizedText address,
    String district,
    Location location,
    int avgStayMinutes,
    String phone,
    String homepageUrl,
    String naverMapUrl,
    List<String> referenceLinks,
    List<String> imageKeys,
    AdminPlaceOperatingPeriod operatingPeriod,
    List<PlaceOpeningHourResponse> openingHours,
    List<String> keywordCodes,
    List<ArtistLink> artists,
    Instant updatedAt) {}
