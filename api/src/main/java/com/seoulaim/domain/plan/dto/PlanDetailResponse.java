package com.seoulaim.domain.plan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
 * </ul>
 */
public record PlanDetailResponse(
    UUID id, String title, LocalDate startDate, LocalDate endDate, List<Day> days) {

  public record Day(int dayNumber, LocalDate visitDate, List<Item> items) {}

  public record Item(
      UUID id,
      int sequence,
      @JsonFormat(pattern = "HH:mm") LocalTime arrivalTime,
      PlaceSummary place) {}

  public record PlaceSummary(
      UUID id,
      String name,
      String address,
      Location location,
      List<String> imageKeys,
      List<ArtistTag> artists) {}

  public record Location(double latitude, double longitude) {}

  // 그룹 단위 연결이면 artistNameEn null
  public record ArtistTag(String artistGroupNameEn, String artistNameEn, String description) {}

  public static PlanDetailResponse of(
      TravelPlan plan,
      String title,
      List<PlanDay> days,
      Map<UUID, List<PlanItem>> itemsByDayId,
      Map<UUID, List<String>> imageKeysByPlaceId,
      Map<UUID, List<ArtistPlace>> artistPlacesByPlaceId) {
    List<Day> dayResponses =
        days.stream()
            .map(
                day ->
                    new Day(
                        day.getDayNumber(),
                        day.getVisitDate(),
                        itemsByDayId.getOrDefault(day.getId(), List.of()).stream()
                            .map(item -> toItem(item, imageKeysByPlaceId, artistPlacesByPlaceId))
                            .toList()))
            .toList();
    return new PlanDetailResponse(
        plan.getId(), title, plan.getStartDate(), plan.getEndDate(), dayResponses);
  }

  private static Item toItem(
      PlanItem item,
      Map<UUID, List<String>> imageKeysByPlaceId,
      Map<UUID, List<ArtistPlace>> artistPlacesByPlaceId) {
    Place place = item.getPlace();
    List<ArtistTag> artists =
        artistPlacesByPlaceId.getOrDefault(place.getId(), List.of()).stream()
            .map(
                artistPlace ->
                    new ArtistTag(
                        artistPlace.getArtistGroup().getNameEn(),
                        artistPlace.getArtist() == null
                            ? null
                            : artistPlace.getArtist().getNameEn(),
                        artistPlace.getDescription()))
            .toList();
    // Point 의 x = 경도, y = 위도
    PlaceSummary summary =
        new PlaceSummary(
            place.getId(),
            place.getName(),
            place.getAddress(),
            new Location(place.getLocation().getY(), place.getLocation().getX()),
            imageKeysByPlaceId.getOrDefault(place.getId(), List.of()),
            artists);
    return new Item(item.getId(), item.getSequence(), item.getArrivalTime(), summary);
  }
}
