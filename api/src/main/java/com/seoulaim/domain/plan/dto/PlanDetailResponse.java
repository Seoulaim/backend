package com.seoulaim.domain.plan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.seoulaim.domain.place.dto.PlaceDetailResponse.Location;
import com.seoulaim.domain.place.entity.ArtistPlace;
import com.seoulaim.domain.place.entity.LocalizedText;
import com.seoulaim.domain.place.entity.Place;
import com.seoulaim.domain.plan.entity.PlanArtist;
import com.seoulaim.domain.plan.entity.PlanDay;
import com.seoulaim.domain.plan.entity.PlanItem;
import com.seoulaim.domain.plan.entity.TravelPlan;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 일정 상세 (HT02, HT10)
 *
 * <ul>
 *   <li>title: 저장값 아님, "{닉네임}'s Travel"
 *   <li>biases: 일정 생성 때 고른 아티스트, 요청 순서. 이 기능 이전에 만든 일정은 빈 배열
 *   <li>days[].items: 방문 순서, 지도 경로도 이 순서로 연결
 *   <li>안쪽 record 이름은 Swagger 스키마 이름 → 다른 도메인과 겹치지 않게 Plan 접두사
 * </ul>
 */
public record PlanDetailResponse(
    UUID id,
    String title,
    LocalDate startDate,
    LocalDate endDate,
    List<PlanBiasResponse> biases,
    List<PlanDayResponse> days) {

  public record PlanBiasResponse(
      UUID artistId, String artistNameEn, UUID artistGroupId, String artistGroupNameEn) {

    static PlanBiasResponse from(PlanArtist planArtist) {
      return new PlanBiasResponse(
          planArtist.getArtist().getId(),
          planArtist.getArtist().getNameEn(),
          planArtist.getArtist().getArtistGroup().getId(),
          planArtist.getArtist().getArtistGroup().getNameEn());
    }
  }

  public record PlanDayResponse(int dayNumber, LocalDate visitDate, List<PlanItemResponse> items) {}

  public record PlanItemResponse(
      UUID id,
      int sequence,
      @JsonFormat(pattern = "HH:mm") LocalTime arrivalTime,
      PlanPlaceResponse place) {}

  // name·address: 한국어(없으면 영어) 기본 표시값, names·summaries: 언어별 이름·주제
  public record PlanPlaceResponse(
      UUID id,
      String name,
      LocalizedText names,
      LocalizedText summaries,
      String address,
      Location location,
      List<String> imageKeys,
      List<PlanArtistTag> artists) {}

  // 그룹 단위 연결이면 artistId·artistNameEn null
  public record PlanArtistTag(
      UUID artistGroupId,
      String artistGroupNameEn,
      UUID artistId,
      String artistNameEn,
      String description) {}

  public static PlanDetailResponse of(
      TravelPlan plan,
      String title,
      List<PlanArtist> planArtists,
      List<PlanDay> days,
      Map<UUID, List<PlanItem>> itemsByDayId,
      Map<UUID, List<String>> imageKeysByPlaceId,
      Map<UUID, List<ArtistPlace>> artistPlacesByPlaceId) {
    List<PlanDayResponse> dayResponses =
        days.stream()
            .map(
                day ->
                    new PlanDayResponse(
                        day.getDayNumber(),
                        day.getVisitDate(),
                        itemsByDayId.getOrDefault(day.getId(), List.of()).stream()
                            .map(item -> toItem(item, imageKeysByPlaceId, artistPlacesByPlaceId))
                            .toList()))
            .toList();
    return new PlanDetailResponse(
        plan.getId(),
        title,
        plan.getStartDate(),
        plan.getEndDate(),
        planArtists.stream().map(PlanBiasResponse::from).toList(),
        dayResponses);
  }

  private static PlanItemResponse toItem(
      PlanItem item,
      Map<UUID, List<String>> imageKeysByPlaceId,
      Map<UUID, List<ArtistPlace>> artistPlacesByPlaceId) {
    Place place = item.getPlace();
    List<PlanArtistTag> artists =
        artistPlacesByPlaceId.getOrDefault(place.getId(), List.of()).stream()
            .map(
                artistPlace ->
                    new PlanArtistTag(
                        artistPlace.getArtistGroup().getId(),
                        artistPlace.getArtistGroup().getNameEn(),
                        artistPlace.getArtist() == null ? null : artistPlace.getArtist().getId(),
                        artistPlace.getArtist() == null
                            ? null
                            : artistPlace.getArtist().getNameEn(),
                        artistPlace.getDescription()))
            .toList();
    // Point 의 x = 경도, y = 위도
    PlanPlaceResponse summary =
        new PlanPlaceResponse(
            place.getId(),
            place.getName(),
            place.getNames(),
            place.getSummaries(),
            place.getAddress(),
            new Location(place.getLocation().getY(), place.getLocation().getX()),
            imageKeysByPlaceId.getOrDefault(place.getId(), List.of()),
            artists);
    return new PlanItemResponse(item.getId(), item.getSequence(), item.getArrivalTime(), summary);
  }
}
