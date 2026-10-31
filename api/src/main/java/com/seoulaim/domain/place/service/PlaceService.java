package com.seoulaim.domain.place.service;

import com.seoulaim.domain.place.dto.PlaceDetailResponse;
import com.seoulaim.domain.place.entity.Place;
import com.seoulaim.domain.place.entity.PlaceEvent;
import com.seoulaim.domain.place.entity.PlaceImage;
import com.seoulaim.domain.place.repository.ArtistPlaceRepository;
import com.seoulaim.domain.place.repository.PlaceEventRepository;
import com.seoulaim.domain.place.repository.PlaceFavoriteRepository;
import com.seoulaim.domain.place.repository.PlaceImageRepository;
import com.seoulaim.domain.place.repository.PlaceRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 장소 조회 */
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
}
