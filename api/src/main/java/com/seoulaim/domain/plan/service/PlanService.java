package com.seoulaim.domain.plan.service;

import com.seoulaim.domain.artist.entity.Artist;
import com.seoulaim.domain.artist.service.ArtistService;
import com.seoulaim.domain.artist.service.UserBiasService;
import com.seoulaim.domain.place.entity.ArtistPlace;
import com.seoulaim.domain.place.entity.Place;
import com.seoulaim.domain.place.entity.PlaceEvent;
import com.seoulaim.domain.place.entity.PlaceKeyword;
import com.seoulaim.domain.place.service.KeywordService;
import com.seoulaim.domain.place.service.PlaceService;
import com.seoulaim.domain.plan.dto.CreatePlanRequest;
import com.seoulaim.domain.plan.dto.PlanDetailResponse;
import com.seoulaim.domain.plan.dto.PlanSummaryResponse;
import com.seoulaim.domain.plan.dto.RecommendDatesRequest;
import com.seoulaim.domain.plan.dto.RecommendDatesResponse;
import com.seoulaim.domain.plan.entity.PlanDay;
import com.seoulaim.domain.plan.entity.PlanItem;
import com.seoulaim.domain.plan.entity.TravelPlan;
import com.seoulaim.domain.plan.repository.PlanDayRepository;
import com.seoulaim.domain.plan.repository.PlanItemRepository;
import com.seoulaim.domain.plan.repository.TravelPlanRepository;
import com.seoulaim.domain.plan.service.PlanRecommender.Candidate;
import com.seoulaim.domain.plan.service.PlanRecommender.Stop;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.service.UserService;
import com.seoulaim.global.common.CursorPageResponse;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 여행 일정 추천·생성·조회·삭제
 *
 * <ul>
 *   <li>날짜 기준은 KST
 *   <li>입력한 bias·키워드는 저장하지 않음
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PlanService {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final int MAX_PERIOD_DAYS = 30;
  private static final String TITLE_SUFFIX = "'s Travel";

  private final TravelPlanRepository travelPlanRepository;
  private final PlanDayRepository planDayRepository;
  private final PlanItemRepository planItemRepository;
  private final UserService userService;
  private final ArtistService artistService;
  private final UserBiasService userBiasService;
  private final KeywordService keywordService;
  private final PlaceService placeService;
  private final PlanRecommender planRecommender;
  private final Clock clock;

  // 기간 안에서 bias 관련 이벤트가 가장 많은 날 dayCount 개 (동점이면 빠른 날)
  public RecommendDatesResponse recommendDates(UUID userId, RecommendDatesRequest request) {
    LocalDate startDate = request.startDate();
    LocalDate endDate = request.endDate();
    validatePeriod(startDate, endDate);
    if (request.dayCount() > ChronoUnit.DAYS.between(startDate, endDate) + 1) {
      throw new BusinessException(ErrorCode.PLAN_INVALID_PERIOD);
    }

    List<UUID> artistIds =
        request.artistIdsOrEmpty().isEmpty()
            ? userBiasService.findMyBiasArtistIds(userId)
            : request.artistIdsOrEmpty();
    List<LocalDate> dates = startDate.datesUntil(endDate.plusDays(1)).toList();

    Map<LocalDate, Long> eventCounts = new HashMap<>();
    if (!artistIds.isEmpty()) {
      List<Artist> artists = artistService.getArtistsWithGroup(artistIds);
      Set<UUID> placeIds =
          placeService.findArtistPlacesByArtists(artistIds, groupIds(artists)).stream()
              .map(artistPlace -> artistPlace.getPlace().getId())
              .collect(Collectors.toSet());
      for (PlaceEvent event : placeService.findEventsInPeriod(placeIds, startDate, endDate)) {
        dates.stream()
            .filter(event::isOngoingOn)
            .forEach(date -> eventCounts.merge(date, 1L, Long::sum));
      }
    }

    List<LocalDate> picked =
        dates.stream()
            .sorted(
                Comparator.<LocalDate, Long>comparing(date -> eventCounts.getOrDefault(date, 0L))
                    .reversed()
                    .thenComparing(Comparator.naturalOrder()))
            .limit(request.dayCount())
            .sorted()
            .toList();
    return new RecommendDatesResponse(picked);
  }

  @Transactional
  public PlanDetailResponse createPlan(UUID userId, CreatePlanRequest request) {
    LocalDate startDate = request.startDate();
    LocalDate endDate = request.endDate();
    validatePeriod(startDate, endDate);

    List<LocalDate> visitDates = request.visitDates().stream().sorted().toList();
    if (new HashSet<>(visitDates).size() != visitDates.size()) {
      throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST);
    }
    if (visitDates.stream().anyMatch(date -> date.isBefore(startDate) || date.isAfter(endDate))) {
      throw new BusinessException(ErrorCode.PLAN_VISIT_DATE_OUT_OF_PERIOD);
    }

    List<Artist> artists = artistService.getArtistsWithGroup(request.artistIds());
    List<UUID> keywordIds = request.keywordIdsOrEmpty();
    keywordService.validateKeywordsExist(keywordIds);

    Map<LocalDate, List<Stop>> schedule =
        planRecommender.recommend(
            buildCandidates(artists, keywordIds, startDate, endDate), visitDates);
    if (schedule.values().stream().allMatch(List::isEmpty)) {
      throw new BusinessException(ErrorCode.PLAN_NO_CANDIDATE_PLACE);
    }

    User user = userService.getUser(userId);
    TravelPlan plan = travelPlanRepository.save(TravelPlan.create(user, startDate, endDate));
    int dayNumber = 1;
    for (Map.Entry<LocalDate, List<Stop>> entry : schedule.entrySet()) {
      PlanDay day = planDayRepository.save(PlanDay.create(plan, dayNumber++, entry.getKey()));
      int sequence = 1;
      for (Stop stop : entry.getValue()) {
        planItemRepository.save(PlanItem.create(day, stop.place(), sequence++, stop.arrivalTime()));
      }
    }
    return getPlanDetail(userId, plan.getId());
  }

  public CursorPageResponse<PlanSummaryResponse> findPlans(UUID userId, String cursor, int size) {
    Limit limit = Limit.of(size + 1);
    List<TravelPlan> plans =
        cursor == null || cursor.isBlank()
            ? travelPlanRepository.findAllByUserIdOrderByIdDesc(userId, limit)
            : travelPlanRepository.findAllByUserIdAndIdLessThanOrderByIdDesc(
                userId, parseCursor(cursor), limit);

    boolean hasNext = plans.size() > size;
    List<TravelPlan> page = hasNext ? plans.subList(0, size) : plans;
    if (page.isEmpty()) {
      return new CursorPageResponse<>(List.of(), null);
    }

    Map<UUID, Long> dayCounts =
        planDayRepository.countByPlanIds(page.stream().map(TravelPlan::getId).toList()).stream()
            .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
    String title = titleOf(userId);

    List<PlanSummaryResponse> items =
        page.stream()
            .map(
                plan ->
                    new PlanSummaryResponse(
                        plan.getId(),
                        title,
                        plan.getStartDate(),
                        plan.getEndDate(),
                        dayCounts.getOrDefault(plan.getId(), 0L).intValue(),
                        plan.getCreatedAt()))
            .toList();
    String nextCursor = hasNext ? page.getLast().getId().toString() : null;
    return new CursorPageResponse<>(items, nextCursor);
  }

  public PlanDetailResponse getPlanDetail(UUID userId, UUID planId) {
    TravelPlan plan = getOwnedPlan(userId, planId);
    List<PlanDay> days = planDayRepository.findAllByPlanIdOrderByDayNumberAsc(planId);
    List<PlanItem> items = planItemRepository.findAllWithPlaceByPlanId(planId);
    Set<UUID> placeIds =
        items.stream().map(item -> item.getPlace().getId()).collect(Collectors.toSet());
    Map<UUID, List<PlanItem>> itemsByDayId =
        items.stream().collect(Collectors.groupingBy(item -> item.getPlanDay().getId()));

    return PlanDetailResponse.of(
        plan,
        titleOf(userId),
        days,
        itemsByDayId,
        placeService.findImageKeysByPlaceIds(placeIds),
        placeService.findArtistPlacesByPlaceIds(placeIds));
  }

  @Transactional
  public void deletePlan(UUID userId, UUID planId) {
    TravelPlan plan = getOwnedPlan(userId, planId);
    planItemRepository.deleteAllByPlanId(planId);
    planDayRepository.deleteAllByPlanId(planId);
    travelPlanRepository.delete(plan);
  }

  // 아티스트 연결 + 키워드 일치 장소를 모아 점수 합산
  private List<Candidate> buildCandidates(
      List<Artist> artists, List<UUID> keywordIds, LocalDate startDate, LocalDate endDate) {
    Map<UUID, Place> places = new LinkedHashMap<>();
    Map<UUID, Integer> scores = new HashMap<>();

    List<UUID> artistIds = artists.stream().map(Artist::getId).toList();
    for (ArtistPlace artistPlace :
        placeService.findArtistPlacesByArtists(artistIds, groupIds(artists))) {
      Place place = artistPlace.getPlace();
      places.putIfAbsent(place.getId(), place);
      int score =
          artistPlace.getArtist() == null
              ? PlanRecommender.GROUP_SCORE
              : PlanRecommender.ARTIST_SCORE;
      scores.merge(place.getId(), score, Integer::sum);
    }
    for (PlaceKeyword placeKeyword : placeService.findPlaceKeywordsByKeywords(keywordIds)) {
      Place place = placeKeyword.getPlace();
      places.putIfAbsent(place.getId(), place);
      scores.merge(place.getId(), PlanRecommender.KEYWORD_SCORE, Integer::sum);
    }

    Map<UUID, List<PlaceEvent>> eventsByPlaceId =
        placeService.findEventsInPeriod(places.keySet(), startDate, endDate).stream()
            .collect(Collectors.groupingBy(event -> event.getPlace().getId()));

    return places.values().stream()
        .map(
            place ->
                new Candidate(
                    place,
                    scores.get(place.getId()),
                    eventsByPlaceId.getOrDefault(place.getId(), List.of())))
        .toList();
  }

  private Set<UUID> groupIds(List<Artist> artists) {
    return artists.stream()
        .map(artist -> artist.getArtistGroup().getId())
        .collect(Collectors.toSet());
  }

  // 시작일은 오늘(KST) 이후, 종료일은 시작일 이후, 최대 30일
  private void validatePeriod(LocalDate startDate, LocalDate endDate) {
    LocalDate today = LocalDate.ofInstant(clock.instant(), KST);
    if (startDate.isBefore(today)
        || endDate.isBefore(startDate)
        || ChronoUnit.DAYS.between(startDate, endDate) + 1 > MAX_PERIOD_DAYS) {
      throw new BusinessException(ErrorCode.PLAN_INVALID_PERIOD);
    }
  }

  // 없는 일정과 다른 유저의 일정은 같은 응답 (존재 여부 노출 방지)
  private TravelPlan getOwnedPlan(UUID userId, UUID planId) {
    return travelPlanRepository
        .findByIdAndUserId(planId, userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));
  }

  private String titleOf(UUID userId) {
    return userService.getUser(userId).getNickname() + TITLE_SUFFIX;
  }

  private UUID parseCursor(String cursor) {
    try {
      return UUID.fromString(cursor);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(ErrorCode.PLAN_INVALID_CURSOR);
    }
  }
}
