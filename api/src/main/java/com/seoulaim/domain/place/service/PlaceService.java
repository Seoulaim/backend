package com.seoulaim.domain.place.service;

import com.seoulaim.domain.place.dto.PlaceDetailResponse;
import com.seoulaim.domain.place.entity.ArtistPlace;
import com.seoulaim.domain.place.entity.Place;
import com.seoulaim.domain.place.entity.PlaceEvent;
import com.seoulaim.domain.place.entity.PlaceImage;
import com.seoulaim.domain.place.entity.PlaceKeyword;
import com.seoulaim.domain.place.repository.ArtistPlaceRepository;
import com.seoulaim.domain.place.repository.PlaceEventRepository;
import com.seoulaim.domain.place.repository.PlaceFavoriteRepository;
import com.seoulaim.domain.place.repository.PlaceImageRepository;
import com.seoulaim.domain.place.repository.PlaceKeywordRepository;
import com.seoulaim.domain.place.repository.PlaceRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
  private final ArtistPlaceRepository artistPlaceRepository;
  private final PlaceFavoriteRepository placeFavoriteRepository;
  private final PlaceKeywordRepository placeKeywordRepository;
  private final Clock clock;

  public Place getPlace(UUID placeId) {
    return placeRepository
        .findById(placeId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
  }

  public PlaceDetailResponse getPlaceDetail(UUID userId, UUID placeId) {
    Place place = getPlace(placeId);
    LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), KST);

    PlaceEvent ongoingEvent =
        placeEventRepository.findOngoingByPlaceId(placeId, now.toLocalDate()).stream()
            .findFirst()
            .orElse(null);
    // 이벤트에 운영시간이 있으면 이벤트 시간, 없으면 상시 영업시간
    boolean useEventHours =
        ongoingEvent != null
            && ongoingEvent.getOpensAt() != null
            && ongoingEvent.getClosesAt() != null;
    LocalTime opensAt = useEventHours ? ongoingEvent.getOpensAt() : place.getOpensAt();
    LocalTime closesAt = useEventHours ? ongoingEvent.getClosesAt() : place.getClosesAt();

    List<String> imageKeys =
        placeImageRepository.findAllByPlaceIdOrderByDisplayOrderAscIdAsc(placeId).stream()
            .map(PlaceImage::getImageKey)
            .toList();

    return PlaceDetailResponse.of(
        place,
        opensAt,
        closesAt,
        OpeningHours.isOpen(opensAt, closesAt, now.toLocalTime()),
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
}
