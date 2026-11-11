package com.seoulaim.domain.place.service;

import com.seoulaim.domain.artist.entity.Artist;
import com.seoulaim.domain.artist.entity.ArtistGroup;
import com.seoulaim.domain.artist.repository.ArtistGroupRepository;
import com.seoulaim.domain.artist.repository.ArtistRepository;
import com.seoulaim.domain.file.entity.FileDomain;
import com.seoulaim.domain.file.service.FileService;
import com.seoulaim.domain.place.admin.dto.AdminPlaceDetailResponse;
import com.seoulaim.domain.place.admin.dto.AdminPlaceListRequest;
import com.seoulaim.domain.place.admin.dto.AdminPlaceListResponse;
import com.seoulaim.domain.place.admin.dto.AdminPlaceOperatingPeriod;
import com.seoulaim.domain.place.admin.dto.AdminPlaceRequest;
import com.seoulaim.domain.place.admin.dto.AdminPlaceRequest.AdminPlaceArtistRequest;
import com.seoulaim.domain.place.admin.dto.AdminPlaceRequest.AdminPlaceOpeningHourRequest;
import com.seoulaim.domain.place.admin.dto.AdminPlaceSummaryResponse;
import com.seoulaim.domain.place.dto.PlaceDetailResponse.ArtistLink;
import com.seoulaim.domain.place.dto.PlaceDetailResponse.Location;
import com.seoulaim.domain.place.dto.PlaceOpeningHourResponse;
import com.seoulaim.domain.place.entity.ArtistPlace;
import com.seoulaim.domain.place.entity.Keyword;
import com.seoulaim.domain.place.entity.Place;
import com.seoulaim.domain.place.entity.PlaceEvent;
import com.seoulaim.domain.place.entity.PlaceImage;
import com.seoulaim.domain.place.entity.PlaceKeyword;
import com.seoulaim.domain.place.entity.PlaceOpeningHour;
import com.seoulaim.domain.place.entity.Weekday;
import com.seoulaim.domain.place.repository.ArtistPlaceRepository;
import com.seoulaim.domain.place.repository.KeywordRepository;
import com.seoulaim.domain.place.repository.PlaceEventRepository;
import com.seoulaim.domain.place.repository.PlaceFavoriteRepository;
import com.seoulaim.domain.place.repository.PlaceImageRepository;
import com.seoulaim.domain.place.repository.PlaceKeywordRepository;
import com.seoulaim.domain.place.repository.PlaceOpeningHourRepository;
import com.seoulaim.domain.place.repository.PlaceRepository;
import com.seoulaim.domain.plan.repository.PlanItemRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 어드민 장소 관리
 *
 * <ul>
 *   <li>등록·수정은 전체 교체: 사진·운영 기간·영업시간·키워드·멤버 태그를 지우고 다시 저장
 *   <li>운영 기간은 place_events 에 장소당 1건 (이벤트 운영시간 없음 → 요일 영업시간 사용)
 *   <li>새로 추가된 사진 key 만 파일 연결 처리
 *   <li>사용자 일정에 포함된 장소는 삭제 불가 → 숨김 처리로 대체
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminPlaceService {

  // Point 는 (x = 경도, y = 위도)
  private static final GeometryFactory GEOMETRY = new GeometryFactory(new PrecisionModel(), 4326);
  private static final int OPENING_HOUR_DAYS = 7;

  private final PlaceRepository placeRepository;
  private final PlaceImageRepository placeImageRepository;
  private final PlaceEventRepository placeEventRepository;
  private final PlaceOpeningHourRepository placeOpeningHourRepository;
  private final PlaceKeywordRepository placeKeywordRepository;
  private final ArtistPlaceRepository artistPlaceRepository;
  private final PlaceFavoriteRepository placeFavoriteRepository;
  private final KeywordRepository keywordRepository;
  private final ArtistGroupRepository artistGroupRepository;
  private final ArtistRepository artistRepository;
  private final PlanItemRepository planItemRepository;
  private final FileService fileService;

  // 최신 등록순 커서 페이징
  public AdminPlaceListResponse getPlaces(AdminPlaceListRequest request) {
    if (request.untaggedOnly() && request.artistGroupId() != null) {
      throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST);
    }
    Specification<Place> filter = filter(request);
    long totalCount = placeRepository.count(filter);

    Specification<Place> pageFilter = filter;
    if (request.cursor() != null && !request.cursor().isBlank()) {
      UUID cursorId = parseCursor(request.cursor());
      pageFilter = filter.and((root, query, cb) -> cb.lessThan(root.<UUID>get("id"), cursorId));
    }
    int size = request.sizeOrDefault();
    List<Place> places =
        placeRepository
            .findAll(pageFilter, PageRequest.of(0, size + 1, Sort.by(Sort.Direction.DESC, "id")))
            .getContent();

    boolean hasNext = places.size() > size;
    List<Place> page = hasNext ? places.subList(0, size) : places;
    String nextCursor = hasNext ? page.getLast().getId().toString() : null;
    return new AdminPlaceListResponse(toSummaries(page), totalCount, nextCursor);
  }

  public AdminPlaceDetailResponse getPlace(UUID placeId) {
    return toDetail(findPlace(placeId));
  }

  @Transactional
  public AdminPlaceDetailResponse createPlace(AdminPlaceRequest request) {
    validate(request);
    List<Keyword> keywords = resolveKeywords(request.keywordCodesOrEmpty());
    List<ArtistTarget> artistTargets = resolveArtists(request.artistsOrEmpty());
    fileService.markLinkedByS3Keys(request.imageKeysOrEmpty(), FileDomain.PLACE);

    Place place =
        Place.create(
            request.name().en(),
            request.categoryOrDefault(),
            request.address().ko(),
            toPoint(request),
            request.district(),
            request.phone(),
            request.homepageUrl());
    applyFields(place, request);
    Place saved = placeRepository.save(place);

    saveChildren(saved, request, keywords, artistTargets);
    return toDetail(saved);
  }

  @Transactional
  public AdminPlaceDetailResponse updatePlace(UUID placeId, AdminPlaceRequest request) {
    Place place = findPlace(placeId);
    validate(request);
    List<Keyword> keywords = resolveKeywords(request.keywordCodesOrEmpty());
    List<ArtistTarget> artistTargets = resolveArtists(request.artistsOrEmpty());

    Set<String> currentImageKeys =
        placeImageRepository.findAllByPlaceIdOrderByDisplayOrderAscIdAsc(placeId).stream()
            .map(PlaceImage::getImageKey)
            .collect(Collectors.toSet());
    fileService.markLinkedByS3Keys(
        request.imageKeysOrEmpty().stream().filter(key -> !currentImageKeys.contains(key)).toList(),
        FileDomain.PLACE);

    deleteChildren(placeId);
    applyFields(place, request);
    saveChildren(place, request, keywords, artistTargets);
    // updatedAt 반영 후 응답
    placeRepository.flush();
    return toDetail(place);
  }

  // 노출 토글. 전체 PUT 과 달리 다른 필드(영어 요약·주소 등)를 검증하지 않는다
  @Transactional
  public AdminPlaceDetailResponse changeVisibility(UUID placeId, boolean visible) {
    Place place = findPlace(placeId);
    place.changeVisibility(visible);
    // updatedAt 반영 후 응답
    placeRepository.flush();
    return toDetail(place);
  }

  @Transactional
  public void deletePlace(UUID placeId) {
    Place place = findPlace(placeId);
    if (planItemRepository.existsByPlaceId(placeId)) {
      throw new BusinessException(ErrorCode.PLACE_IN_USE);
    }
    placeFavoriteRepository.deleteAllByPlaceId(placeId);
    deleteChildren(placeId);
    placeRepository.delete(place);
  }

  // ---------- 저장 ----------

  private record ArtistTarget(ArtistGroup group, Artist artist, String description) {}

  private void applyFields(Place place, AdminPlaceRequest request) {
    place.update(
        request.name().toLocalized(),
        request.summary().toLocalized(),
        request.categoryOrDefault(),
        request.visibleOrDefault(),
        request.address().toLocalized(),
        toPoint(request),
        request.district(),
        request.avgStayMinutesOrDefault(),
        request.phone(),
        request.homepageUrl(),
        request.naverMapUrl(),
        request.referenceLinksOrEmpty());
  }

  private void saveChildren(
      Place place,
      AdminPlaceRequest request,
      List<Keyword> keywords,
      List<ArtistTarget> artistTargets) {
    List<String> imageKeys = request.imageKeysOrEmpty();
    for (int order = 0; order < imageKeys.size(); order++) {
      placeImageRepository.save(PlaceImage.create(place, imageKeys.get(order), order));
    }

    AdminPlaceOperatingPeriod period = request.operatingPeriod();
    if (period != null) {
      placeEventRepository.save(
          PlaceEvent.create(
              place, place.getName(), period.startsOn(), period.endsOn(), null, null));
    }

    if (request.openingHours() != null) {
      for (AdminPlaceOpeningHourRequest hour : request.openingHours()) {
        placeOpeningHourRepository.save(
            hour.closed()
                ? PlaceOpeningHour.closedOn(place, hour.dayOfWeek())
                : PlaceOpeningHour.open(
                    place,
                    hour.dayOfWeek(),
                    hour.opensAt(),
                    hour.closesAt(),
                    hour.breakStartsAt(),
                    hour.breakEndsAt()));
      }
    }

    keywords.forEach(keyword -> placeKeywordRepository.save(PlaceKeyword.create(place, keyword)));

    for (ArtistTarget target : artistTargets) {
      artistPlaceRepository.save(
          target.artist() == null
              ? ArtistPlace.ofGroup(place, target.group(), target.description())
              : ArtistPlace.ofArtist(place, target.artist(), target.description()));
    }
  }

  // 즐겨찾기 제외한 하위 데이터
  private void deleteChildren(UUID placeId) {
    placeImageRepository.deleteAllByPlaceId(placeId);
    placeEventRepository.deleteAllByPlaceId(placeId);
    placeOpeningHourRepository.deleteAllByPlaceId(placeId);
    placeKeywordRepository.deleteAllByPlaceId(placeId);
    artistPlaceRepository.deleteAllByPlaceId(placeId);
  }

  // ---------- 검증 ----------

  private void validate(AdminPlaceRequest request) {
    AdminPlaceOperatingPeriod period = request.operatingPeriod();
    if (period != null && period.startsOn().isAfter(period.endsOn())) {
      throw new BusinessException(ErrorCode.PLACE_INVALID_OPERATING_PERIOD);
    }
    if (request.openingHours() != null && !isValidOpeningHours(request.openingHours())) {
      throw new BusinessException(ErrorCode.PLACE_INVALID_OPENING_HOURS);
    }
  }

  // MON ~ SUN 7개, 휴무일은 시각 없음, 영업일은 OpeningHours.isValid (자정 넘김·브레이크 범위)
  private boolean isValidOpeningHours(List<AdminPlaceOpeningHourRequest> hours) {
    Set<Weekday> days =
        hours.stream().map(AdminPlaceOpeningHourRequest::dayOfWeek).collect(Collectors.toSet());
    if (hours.size() != OPENING_HOUR_DAYS || days.size() != OPENING_HOUR_DAYS) {
      return false;
    }
    for (AdminPlaceOpeningHourRequest hour : hours) {
      if (hour.closed()) {
        if (hour.opensAt() != null
            || hour.closesAt() != null
            || hour.breakStartsAt() != null
            || hour.breakEndsAt() != null) {
          return false;
        }
      } else if (!OpeningHours.isValid(
          hour.opensAt(), hour.closesAt(), hour.breakStartsAt(), hour.breakEndsAt())) {
        return false;
      }
    }
    return true;
  }

  private List<Keyword> resolveKeywords(List<String> codes) {
    Set<String> uniqueCodes = new LinkedHashSet<>(codes);
    if (uniqueCodes.isEmpty()) {
      return List.of();
    }
    List<Keyword> keywords = keywordRepository.findAllByCodeIn(uniqueCodes);
    if (keywords.size() != uniqueCodes.size()) {
      throw new BusinessException(ErrorCode.PLACE_KEYWORD_NOT_FOUND);
    }
    return keywords;
  }

  private List<ArtistTarget> resolveArtists(List<AdminPlaceArtistRequest> requests) {
    if (requests.isEmpty()) {
      return List.of();
    }
    Set<UUID> groupIds =
        requests.stream().map(AdminPlaceArtistRequest::artistGroupId).collect(Collectors.toSet());
    Map<UUID, ArtistGroup> groups =
        artistGroupRepository.findAllById(groupIds).stream()
            .collect(Collectors.toMap(ArtistGroup::getId, Function.identity()));
    if (groups.size() != groupIds.size()) {
      throw new BusinessException(ErrorCode.ARTIST_GROUP_NOT_FOUND);
    }

    Set<UUID> artistIds =
        requests.stream()
            .map(AdminPlaceArtistRequest::artistId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());
    Map<UUID, Artist> artists =
        artistIds.isEmpty()
            ? Map.of()
            : artistRepository.findAllWithGroupByIdIn(artistIds).stream()
                .collect(Collectors.toMap(Artist::getId, Function.identity()));
    if (artists.size() != artistIds.size()) {
      throw new BusinessException(ErrorCode.ARTIST_NOT_FOUND);
    }

    // 같은 멤버 또는 같은 그룹 단위 연결 중복 방지
    Set<String> linkKeys = new HashSet<>();
    List<ArtistTarget> targets = new ArrayList<>();
    for (AdminPlaceArtistRequest request : requests) {
      Artist artist = request.artistId() == null ? null : artists.get(request.artistId());
      if (artist != null && !artist.getArtistGroup().getId().equals(request.artistGroupId())) {
        throw new BusinessException(ErrorCode.ARTIST_NOT_IN_GROUP);
      }
      String linkKey =
          artist == null ? "group:" + request.artistGroupId() : "artist:" + artist.getId();
      if (!linkKeys.add(linkKey)) {
        throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST);
      }
      targets.add(
          new ArtistTarget(groups.get(request.artistGroupId()), artist, request.description()));
    }
    return targets;
  }

  // ---------- 조회 ----------

  private Place findPlace(UUID placeId) {
    return placeRepository
        .findById(placeId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
  }

  private AdminPlaceDetailResponse toDetail(Place place) {
    UUID placeId = place.getId();
    List<String> imageKeys =
        placeImageRepository.findAllByPlaceIdOrderByDisplayOrderAscIdAsc(placeId).stream()
            .map(PlaceImage::getImageKey)
            .toList();
    List<String> keywordCodes =
        placeKeywordRepository.findAllWithKeywordByPlaceIdIn(List.of(placeId)).stream()
            .map(placeKeyword -> placeKeyword.getKeyword().getCode())
            .toList();
    List<ArtistLink> artists =
        artistPlaceRepository.findAllWithArtistByPlaceId(placeId).stream()
            .map(ArtistLink::from)
            .toList();

    return new AdminPlaceDetailResponse(
        placeId,
        place.getNames(),
        place.getSummaries(),
        place.getCategory(),
        place.isVisible(),
        place.getAddresses(),
        place.getDistrict(),
        new Location(place.getLocation().getY(), place.getLocation().getX()),
        place.getAvgStayMinutes(),
        place.getPhone(),
        place.getHomepageUrl(),
        place.getNaverMapUrl(),
        List.copyOf(place.getReferenceLinks()),
        imageKeys,
        findOperatingPeriods(List.of(placeId)).get(placeId),
        PlaceOpeningHourResponse.listOf(placeOpeningHourRepository.findAllByPlaceId(placeId)),
        keywordCodes,
        artists,
        place.getUpdatedAt());
  }

  private List<AdminPlaceSummaryResponse> toSummaries(List<Place> places) {
    if (places.isEmpty()) {
      return List.of();
    }
    List<UUID> placeIds = places.stream().map(Place::getId).toList();

    Map<UUID, String> thumbnails = new LinkedHashMap<>();
    placeImageRepository
        .findAllByPlaceIdInOrderByDisplayOrderAscIdAsc(placeIds)
        .forEach(image -> thumbnails.putIfAbsent(image.getPlace().getId(), image.getImageKey()));
    Map<UUID, Long> artistCounts = toCountMap(artistPlaceRepository.countByPlaceIds(placeIds));
    Map<UUID, Long> keywordCounts = toCountMap(placeKeywordRepository.countByPlaceIds(placeIds));
    Map<UUID, AdminPlaceOperatingPeriod> periods = findOperatingPeriods(placeIds);
    Map<UUID, List<PlaceOpeningHour>> hours =
        placeOpeningHourRepository.findAllByPlaceIdIn(placeIds).stream()
            .collect(Collectors.groupingBy(hour -> hour.getPlace().getId()));

    return places.stream()
        .map(
            place ->
                new AdminPlaceSummaryResponse(
                    place.getId(),
                    place.getNames(),
                    place.getCategory(),
                    place.isVisible(),
                    thumbnails.get(place.getId()),
                    artistCounts.getOrDefault(place.getId(), 0L),
                    keywordCounts.getOrDefault(place.getId(), 0L),
                    periods.get(place.getId()),
                    PlaceOpeningHourResponse.listOf(hours.getOrDefault(place.getId(), List.of()))))
        .toList();
  }

  // 장소별 운영 기간. 이벤트가 여럿이면 가장 최근 시작한 이벤트
  private Map<UUID, AdminPlaceOperatingPeriod> findOperatingPeriods(Collection<UUID> placeIds) {
    Map<UUID, AdminPlaceOperatingPeriod> periods = new LinkedHashMap<>();
    placeEventRepository
        .findAllByPlaceIdInOrderByStartsOnDescIdDesc(placeIds)
        .forEach(
            event ->
                periods.putIfAbsent(
                    event.getPlace().getId(),
                    new AdminPlaceOperatingPeriod(event.getStartsOn(), event.getEndsOn())));
    return periods;
  }

  private Map<UUID, Long> toCountMap(List<Object[]> rows) {
    return rows.stream().collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
  }

  // ---------- 목록 필터 ----------

  private Specification<Place> filter(AdminPlaceListRequest request) {
    List<Specification<Place>> specs = new ArrayList<>();
    if (request.keyword() != null && !request.keyword().isBlank()) {
      String pattern = "%" + escapeLike(request.keyword().trim().toLowerCase(Locale.ROOT)) + "%";
      specs.add(
          (root, query, cb) ->
              cb.or(
                  Stream.of("nameEn", "nameKo", "nameJa", "nameZh")
                      .map(field -> cb.like(cb.lower(root.<String>get(field)), pattern, '\\'))
                      .toArray(Predicate[]::new)));
    }
    if (request.visible() != null) {
      specs.add((root, query, cb) -> cb.equal(root.get("visible"), request.visible()));
    }
    if (request.category() != null) {
      specs.add((root, query, cb) -> cb.equal(root.get("category"), request.category()));
    }
    if (request.artistGroupId() != null) {
      specs.add(
          (root, query, cb) -> cb.exists(artistLinks(root, query, cb, request.artistGroupId())));
    }
    if (request.untaggedOnly()) {
      specs.add((root, query, cb) -> cb.not(cb.exists(artistLinks(root, query, cb, null))));
    }
    return Specification.allOf(specs);
  }

  // 장소의 멤버 태그. groupId 가 있으면 해당 그룹 태그만
  private static Subquery<UUID> artistLinks(
      Root<Place> root, CriteriaQuery<?> query, CriteriaBuilder cb, UUID groupId) {
    Subquery<UUID> subquery = query.subquery(UUID.class);
    Root<ArtistPlace> artistPlace = subquery.from(ArtistPlace.class);
    Predicate samePlace = cb.equal(artistPlace.get("place"), root);
    subquery
        .select(artistPlace.<UUID>get("id"))
        .where(
            groupId == null
                ? samePlace
                : cb.and(samePlace, cb.equal(artistPlace.get("artistGroup").get("id"), groupId)));
    return subquery;
  }

  private static String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private static Point toPoint(AdminPlaceRequest request) {
    return GEOMETRY.createPoint(
        new Coordinate(request.location().longitude(), request.location().latitude()));
  }

  private UUID parseCursor(String cursor) {
    try {
      return UUID.fromString(cursor);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(ErrorCode.PLACE_INVALID_CURSOR);
    }
  }
}
