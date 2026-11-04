package com.seoulaim.domain.place.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.seoulaim.domain.place.entity.ArtistPlace;
import com.seoulaim.domain.place.entity.LocalizedText;
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
 *   <li>opensAt·closesAt: 오늘(KST) 영업시간. 진행 중 이벤트에 운영시간이 있으면 이벤트 시간 우선, 휴무·영업시간 없음이면 null
 *   <li>openingHours: 요일별 영업시간 7개, 영업시간 없음이면 null
 *   <li>name·address: 한국어(없으면 영어) 기본 표시값. names·summaries·addresses 는 언어별 원본, 빈 언어는 null
 *   <li>artists[].artistId·artistNameEn: 그룹 단위 연결이면 null
 * </ul>
 */
public record PlaceDetailResponse(
    UUID id,
    String name,
    LocalizedText names,
    LocalizedText summaries,
    PlaceCategory category,
    String address,
    LocalizedText addresses,
    Location location,
    String phone,
    String homepageUrl,
    String naverMapUrl,
    @JsonFormat(pattern = "HH:mm") LocalTime opensAt,
    @JsonFormat(pattern = "HH:mm") LocalTime closesAt,
    boolean isOpenNow,
    List<PlaceOpeningHourResponse> openingHours,
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

    public static ArtistLink from(ArtistPlace artistPlace) {
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
      List<PlaceOpeningHourResponse> openingHours,
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
        place.getNames(),
        place.getSummaries(),
        place.getCategory(),
        place.getAddress(),
        place.getAddresses(),
        location,
        place.getPhone(),
        place.getHomepageUrl(),
        place.getNaverMapUrl(),
        opensAt,
        closesAt,
        isOpenNow,
        openingHours,
        event,
        imageKeys,
        artistPlaces.stream().map(ArtistLink::from).toList(),
        isFavorite);
  }
}
