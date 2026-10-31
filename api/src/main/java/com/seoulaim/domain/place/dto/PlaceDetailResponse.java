package com.seoulaim.domain.place.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.seoulaim.domain.place.entity.ArtistPlace;
import com.seoulaim.domain.place.entity.Place;
import com.seoulaim.domain.place.entity.PlaceCategory;
import com.seoulaim.domain.place.entity.PlaceEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * 장소 상세 (HT10)
 *
 * <ul>
 *   <li>opensAt·closesAt: 진행 중 이벤트 운영시간 우선, 없으면 상시 영업시간
 *   <li>artists[].artistId·artistNameEn: 그룹 단위 연결이면 null
 * </ul>
 */
public record PlaceDetailResponse(
    UUID id,
    String name,
    PlaceCategory category,
    String address,
    Location location,
    String phone,
    String homepageUrl,
    @JsonFormat(pattern = "HH:mm") LocalTime opensAt,
    @JsonFormat(pattern = "HH:mm") LocalTime closesAt,
    boolean isOpenNow,
    OngoingEvent ongoingEvent,
    List<String> imageKeys,
    List<ArtistLink> artists,
    boolean isFavorite) {

  public record Location(double latitude, double longitude) {}

  public record OngoingEvent(String title, LocalDate startsOn, LocalDate endsOn) {}

  public record ArtistLink(
      UUID artistGroupId,
      String artistGroupNameEn,
      UUID artistId,
      String artistNameEn,
      String description) {

    static ArtistLink from(ArtistPlace artistPlace) {
      boolean groupLink = artistPlace.getArtist() == null;
      return new ArtistLink(
          artistPlace.getArtistGroup().getId(),
          artistPlace.getArtistGroup().getNameEn(),
          groupLink ? null : artistPlace.getArtist().getId(),
          groupLink ? null : artistPlace.getArtist().getNameEn(),
          artistPlace.getDescription());
    }
  }

  public static PlaceDetailResponse of(
      Place place,
      LocalTime opensAt,
      LocalTime closesAt,
      boolean isOpenNow,
      PlaceEvent ongoingEvent,
      List<String> imageKeys,
      List<ArtistPlace> artistPlaces,
      boolean isFavorite) {
    // Point 의 x = 경도, y = 위도
    Location location = new Location(place.getLocation().getY(), place.getLocation().getX());
    OngoingEvent event =
        ongoingEvent == null
            ? null
            : new OngoingEvent(
                ongoingEvent.getTitle(), ongoingEvent.getStartsOn(), ongoingEvent.getEndsOn());

    return new PlaceDetailResponse(
        place.getId(),
        place.getName(),
        place.getCategory(),
        place.getAddress(),
        location,
        place.getPhone(),
        place.getHomepageUrl(),
        opensAt,
        closesAt,
        isOpenNow,
        event,
        imageKeys,
        artistPlaces.stream().map(ArtistLink::from).toList(),
        isFavorite);
  }
}
