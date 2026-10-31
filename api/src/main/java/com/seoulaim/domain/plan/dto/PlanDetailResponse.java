package com.seoulaim.domain.plan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.seoulaim.domain.place.dto.PlaceDetailResponse.Location;
import com.seoulaim.domain.place.entity.ArtistPlace;
import com.seoulaim.domain.place.entity.Place;
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
 *   <li>days[].items: 방문 순서, 지도 경로도 이 순서로 연결
 *   <li>안쪽 record 이름은 Swagger 스키마 이름 → 다른 도메인과 겹치지 않게 Plan 접두사
 * </ul>
 */
public record PlanDetailResponse(
    UUID id, String title, LocalDate startDate, LocalDate endDate, List<PlanDayResponse> days) {

  public record PlanDayResponse(int dayNumber, LocalDate visitDate, List<PlanItemResponse> items) {}

  public record PlanItemResponse(
      UUID id,
      int sequence,
      @JsonFormat(pattern = "HH:mm") LocalTime arrivalTime,
      PlanPlaceResponse place) {}

  public record PlanPlaceResponse(
      UUID id,
      String name,
      String address,
      Location location,
      List<String> imageKeys,
      List<PlanArtistTag> artists) {}

  // 그룹 단위 연결이면 artistNameEn null
  public record PlanArtistTag(String artistGroupNameEn, String artistNameEn, String description) {}

  public static PlanDetailResponse of(
      TravelPlan plan,
      String title,
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
        plan.getId(), title, plan.getStartDate(), plan.getEndDate(), dayResponses);
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
                        artistPlace.getArtistGroup().getNameEn(),
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
            place.getAddress(),
            new Location(place.getLocation().getY(), place.getLocation().getX()),
            imageKeysByPlaceId.getOrDefault(place.getId(), List.of()),
            artists);
    return new PlanItemResponse(item.getId(), item.getSequence(), item.getArrivalTime(), summary);
  }
}
