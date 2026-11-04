package com.seoulaim.domain.place.admin.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.seoulaim.domain.place.entity.LocalizedText;
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
 *   <li>name·summary: 영어 필수, 나머지 언어는 비면 영어로 표시
 *   <li>address: 한국어(좌표 찾기 기준)·영어 필수
 *   <li>category: 생략 시 FANDOM_PLACE
 *   <li>imageKeys: 어드민 파일 업로드(domain PLACE)로 받은 s3 key, 배열 순서 = 노출 순서 (첫 번째가 대표)
 *   <li>operatingPeriod: null 이면 상시 운영
 *   <li>openingHours: null 이면 영업시간 없음, 있으면 MON ~ SUN 7개. 시각 "HH:mm", "24:00" = 00:00. closesAt ≤
 *       opensAt 이면 다음 날 closesAt 까지 (자정 넘김), 브레이크는 영업 구간 안
 *   <li>artists[].artistId: null 이면 그룹 단위 연결
 * </ul>
 */
public record AdminPlaceRequest(
    @NotNull @Valid AdminPlaceText name,
    @NotNull @Valid AdminPlaceText summary,
    PlaceCategory category,
    Boolean visible,
    @NotNull @Valid AdminPlaceAddress address,
    @Size(max = 50) String district,
    @NotNull @Valid AdminPlaceLocation location,
    @Min(10) @Max(600) Integer avgStayMinutes,
    @Size(max = 50) String phone,
    @Size(max = 512) String homepageUrl,
    @Size(max = 512) String naverMapUrl,
    @Size(max = 5) List<@NotBlank @Size(max = 512) String> referenceLinks,
    @Size(max = 5) List<@NotBlank @Size(max = 512) String> imageKeys,
    @Valid AdminPlaceOperatingPeriod operatingPeriod,
    List<@NotNull @Valid AdminPlaceOpeningHourRequest> openingHours,
    List<@NotBlank String> keywordCodes,
    List<@NotNull @Valid AdminPlaceArtistRequest> artists) {

  private static final int DEFAULT_STAY_MINUTES = 60;

  // 이름·주제. 영어 필수
  public record AdminPlaceText(
      @NotBlank @Size(max = 200) String en,
      @Size(max = 200) String ko,
      @Size(max = 200) String ja,
      @Size(max = 200) String zh) {

    public LocalizedText toLocalized() {
      return new LocalizedText(en, blankToNull(ko), blankToNull(ja), blankToNull(zh));
    }
  }

  // 주소. 한국어·영어 필수
  public record AdminPlaceAddress(
      @NotBlank @Size(max = 500) String ko,
      @NotBlank @Size(max = 500) String en,
      @Size(max = 500) String ja,
      @Size(max = 500) String zh) {

    public LocalizedText toLocalized() {
      return new LocalizedText(en, ko, blankToNull(ja), blankToNull(zh));
    }
  }

  public record AdminPlaceLocation(
      @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
      @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude) {}

  public record AdminPlaceOpeningHourRequest(
      @NotNull Weekday dayOfWeek,
      @NotNull Boolean closed,
      @JsonDeserialize(using = OpeningTimeDeserializer.class) LocalTime opensAt,
      @JsonDeserialize(using = OpeningTimeDeserializer.class) LocalTime closesAt,
      @JsonDeserialize(using = OpeningTimeDeserializer.class) LocalTime breakStartsAt,
      @JsonDeserialize(using = OpeningTimeDeserializer.class) LocalTime breakEndsAt) {}

  public record AdminPlaceArtistRequest(
      @NotNull UUID artistGroupId, UUID artistId, @Size(max = 500) String description) {}

  public PlaceCategory categoryOrDefault() {
    return category == null ? PlaceCategory.FANDOM_PLACE : category;
  }

  public boolean visibleOrDefault() {
    return visible == null || visible;
  }

  public int avgStayMinutesOrDefault() {
    return avgStayMinutes == null ? DEFAULT_STAY_MINUTES : avgStayMinutes;
  }

  public List<String> referenceLinksOrEmpty() {
    return referenceLinks == null ? List.of() : referenceLinks;
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

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
