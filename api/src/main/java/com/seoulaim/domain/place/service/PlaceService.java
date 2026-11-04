package com.seoulaim.domain.place.service;

import com.seoulaim.domain.place.dto.PlaceDetailResponse;
import com.seoulaim.domain.place.dto.PlaceOpeningHourResponse;
import com.seoulaim.domain.place.entity.ArtistPlace;
import com.seoulaim.domain.place.entity.Place;
import com.seoulaim.domain.place.entity.PlaceEvent;
import com.seoulaim.domain.place.entity.PlaceImage;
import com.seoulaim.domain.place.entity.PlaceKeyword;
import com.seoulaim.domain.place.entity.PlaceOpeningHour;
import com.seoulaim.domain.place.entity.Weekday;
import com.seoulaim.domain.place.repository.ArtistPlaceRepository;
import com.seoulaim.domain.place.repository.PlaceEventRepository;
import com.seoulaim.domain.place.repository.PlaceFavoriteRepository;
import com.seoulaim.domain.place.repository.PlaceImageRepository;
import com.seoulaim.domain.place.repository.PlaceKeywordRepository;
import com.seoulaim.domain.place.repository.PlaceOpeningHourRepository;
import com.seoulaim.domain.place.repository.PlaceRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 장소 조회 (상세, 일정 추천·상세용 일괄 조회) */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PlaceService {

  // 영업 여부·진행 중 이벤트는 한국 시각 기준
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final PlaceRepository placeRepository;
  private final PlaceImageRepository placeImageRepository;
  private final PlaceEventRepository placeEventRepository;
  private final PlaceOpeningHourRepository placeOpeningHourRepository;
  private final ArtistPlaceRepository artistPlaceRepository;
  private final PlaceFavoriteRepository placeFavoriteRepository;
  private final PlaceKeywordRepository placeKeywordRepository;
  private final Clock clock;

  // 사용자용 조회. 숨김 장소는 없는 장소와 같은 응답
  public Place getPlace(UUID placeId) {
    return placeRepository
        .findById(placeId)
        .filter(Place::isVisible)
        .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
  }

  public PlaceDetailResponse getPlaceDetail(UUID userId, UUID placeId) {
    Place place = getPlace(placeId);
    LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), KST);
    LocalTime time = now.toLocalTime();

    PlaceEvent ongoingEvent =
        placeEventRepository.findOngoingByPlaceId(placeId, now.toLocalDate()).stream()
            .findFirst()
            .orElse(null);
    List<PlaceOpeningHour> weeklyHours = placeOpeningHourRepository.findAllByPlaceId(placeId);

    LocalTime opensAt;
    LocalTime closesAt;
    boolean isOpenNow;
    if (ongoingEvent != null
        && ongoingEvent.getOpensAt() != null
        && ongoingEvent.getClosesAt() != null) {
      // 이벤트 운영시간 우선
      opensAt = ongoingEvent.getOpensAt();
      closesAt = ongoingEvent.getClosesAt();
      isOpenNow = OpeningHours.isOpen(opensAt, closesAt, time);
    } else if (weeklyHours.isEmpty()) {
      // 영업시간 없음 → 언제든 방문 가능
      opensAt = null;
      closesAt = null;
      isOpenNow = true;
    } else {
      Weekday today = Weekday.from(now.getDayOfWeek());
      Weekday yesterday = Weekday.from(now.minusDays(1).getDayOfWeek());
      PlaceOpeningHour todayHour = openHourOn(weeklyHours, today);
      opensAt = todayHour == null ? null : todayHour.getOpensAt();
      closesAt = todayHour == null ? null : todayHour.getClosesAt();
      // 어제 자정 넘김 영업의 새벽 부분도 영업 중
      isOpenNow = OpeningHours.isOpenNow(todayHour, openHourOn(weeklyHours, yesterday), time);
    }

    List<String> imageKeys =
        placeImageRepository.findAllByPlaceIdOrderByDisplayOrderAscIdAsc(placeId).stream()
            .map(PlaceImage::getImageKey)
            .toList();

    return PlaceDetailResponse.of(
        place,
        opensAt,
        closesAt,
        isOpenNow,
        PlaceOpeningHourResponse.listOf(weeklyHours),
        ongoingEvent,
        imageKeys,
        artistPlaceRepository.findAllWithArtistByPlaceId(placeId),
        placeFavoriteRepository.existsByUserIdAndPlaceId(userId, placeId));
  }

  public List<ArtistPlace> findArtistPlacesByArtists(
      Collection<UUID> artistIds, Collection<UUID> artistGroupIds) {
    return artistPlaceRepository.findAllByArtistsOrGroups(artistIds, artistGroupIds);
  }

  public List<PlaceKeyword> findPlaceKeywordsByKeywords(Collection<UUID> keywordIds) {
    return keywordIds.isEmpty()
        ? List.of()
        : placeKeywordRepository.findAllWithPlaceByKeywordIdIn(keywordIds);
  }

  public List<PlaceEvent> findEventsInPeriod(
      Collection<UUID> placeIds, LocalDate startDate, LocalDate endDate) {
    return placeIds.isEmpty()
        ? List.of()
        : placeEventRepository.findAllInPeriod(placeIds, startDate, endDate);
  }

  // 운영 기간이 있는(기간 한정) 장소 ID
  public Set<UUID> findPeriodLimitedPlaceIds(Collection<UUID> placeIds) {
    return placeIds.isEmpty()
        ? Set.of()
        : new HashSet<>(placeEventRepository.findPlaceIdsHavingEvents(placeIds));
  }

  // 장소별 요일 영업시간. 영업시간 없음인 장소는 결과에 없음
  public Map<UUID, Map<Weekday, PlaceOpeningHour>> findOpeningHoursByPlaceIds(
      Collection<UUID> placeIds) {
    if (placeIds.isEmpty()) {
      return Map.of();
    }
    return placeOpeningHourRepository.findAllByPlaceIdIn(placeIds).stream()
        .collect(
            Collectors.groupingBy(
                hour -> hour.getPlace().getId(),
                Collectors.toMap(PlaceOpeningHour::getDayOfWeek, Function.identity())));
  }

  // 장소별 이미지 키 (display_order 순)
  public Map<UUID, List<String>> findImageKeysByPlaceIds(Collection<UUID> placeIds) {
    if (placeIds.isEmpty()) {
      return Map.of();
    }
    return placeImageRepository.findAllByPlaceIdInOrderByDisplayOrderAscIdAsc(placeIds).stream()
        .collect(
            Collectors.groupingBy(
                image -> image.getPlace().getId(),
                Collectors.mapping(PlaceImage::getImageKey, Collectors.toList())));
  }

  public Map<UUID, List<ArtistPlace>> findArtistPlacesByPlaceIds(Collection<UUID> placeIds) {
    if (placeIds.isEmpty()) {
      return Map.of();
    }
    return artistPlaceRepository.findAllWithArtistByPlaceIdIn(placeIds).stream()
        .collect(Collectors.groupingBy(artistPlace -> artistPlace.getPlace().getId()));
  }

  // 해당 요일의 영업 행 (휴무·없음이면 null)
  private static PlaceOpeningHour openHourOn(List<PlaceOpeningHour> hours, Weekday day) {
    return hours.stream()
        .filter(hour -> hour.getDayOfWeek() == day && !hour.isClosed())
        .findFirst()
        .orElse(null);
  }
}
