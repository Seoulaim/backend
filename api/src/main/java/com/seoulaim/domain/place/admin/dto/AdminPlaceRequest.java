package com.seoulaim.domain.place.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.seoulaim.domain.place.entity.PlaceCategory;
import com.seoulaim.domain.place.entity.Weekday;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * 장소 등록·수정 공용 (전체 교체)
 *
 * <ul>
 *   <li>imageKeys: 어드민 파일 업로드(domain PLACE)로 받은 s3 key, 배열 순서 = 노출 순서 (첫 번째가 대표)
 *   <li>operatingPeriod: null 이면 상시 운영
 *   <li>openingHours: null 이면 영업시간 없음, 있으면 MON ~ SUN 7개
 *   <li>artists[].artistId: null 이면 그룹 단위 연결
 * </ul>
 */
public record AdminPlaceRequest(
    @NotBlank @Size(max = 200) String name,
    @NotNull PlaceCategory category,
    Boolean visible,
    @Size(max = 500) String address,
    @Size(max = 50) String district,
    @NotNull @Valid AdminPlaceLocation location,
    @Min(10) @Max(600) Integer avgStayMinutes,
    @Size(max = 50) String phone,
    @Size(max = 512) String homepageUrl,
    @Size(max = 5) List<@NotBlank @Size(max = 512) String> imageKeys,
    @Valid AdminPlaceOperatingPeriod operatingPeriod,
    List<@NotNull @Valid AdminPlaceOpeningHourRequest> openingHours,
    List<@NotBlank String> keywordCodes,
    List<@NotNull @Valid AdminPlaceArtistRequest> artists) {

  private static final int DEFAULT_STAY_MINUTES = 60;

  public record AdminPlaceLocation(
      @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
      @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude) {}

  public record AdminPlaceOpeningHourRequest(
      @NotNull Weekday dayOfWeek,
      @NotNull Boolean closed,
      @JsonFormat(pattern = "HH:mm") LocalTime opensAt,
      @JsonFormat(pattern = "HH:mm") LocalTime closesAt,
      @JsonFormat(pattern = "HH:mm") LocalTime breakStartsAt,
      @JsonFormat(pattern = "HH:mm") LocalTime breakEndsAt) {}

  public record AdminPlaceArtistRequest(
      @NotNull UUID artistGroupId, UUID artistId, @Size(max = 500) String description) {}

  public boolean visibleOrDefault() {
    return visible == null || visible;
  }

  public int avgStayMinutesOrDefault() {
    return avgStayMinutes == null ? DEFAULT_STAY_MINUTES : avgStayMinutes;
  }

  public List<String> imageKeysOrEmpty() {
    return imageKeys == null ? List.of() : imageKeys;
  }

  public List<String> keywordCodesOrEmpty() {
    return keywordCodes == null ? List.of() : keywordCodes;
  }

  public List<AdminPlaceArtistRequest> artistsOrEmpty() {
    return artists == null ? List.of() : artists;
  }
}
