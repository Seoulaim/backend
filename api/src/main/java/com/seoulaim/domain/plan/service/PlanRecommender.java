package com.seoulaim.domain.plan.service;

import com.seoulaim.domain.place.entity.Place;
import com.seoulaim.domain.place.entity.PlaceEvent;
import com.seoulaim.domain.place.entity.PlaceOpeningHour;
import com.seoulaim.domain.place.entity.Weekday;
import com.seoulaim.domain.place.service.OpeningHours;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 일정 추천 알고리즘 (MVP)
 *
 * <ul>
 *   <li>점수: 멤버 연결 3, 그룹 연결 2, 키워드 일치 3, 당일 이벤트 +3
 *   <li>운영 기간이 있는 장소는 기간 안의 날짜에만 후보
 *   <li>요일 영업시간이 휴무면 그날 후보 제외
 *   <li>날짜별 상위 점수 최대 5곳, 이미 배정한 장소는 다음 날 제외
 *   <li>방문 순서: 1위 장소에서 출발, 남은 곳 중 가장 가까운 곳
 *   <li>이동 시간: 직선거리 ÷ 15km/h + 환승 10분, 10:00 출발 ~ 21:00
 *   <li>여는 시각 전·브레이크 중 도착 → 영업 재개까지 대기
 * </ul>
 */
@Component
public class PlanRecommender {

  static final int ARTIST_SCORE = 3;
  static final int GROUP_SCORE = 2;
  static final int KEYWORD_SCORE = 3;
  private static final int EVENT_BONUS = 3;
  private static final int MAX_PLACES_PER_DAY = 5;
  private static final int DAY_START_MINUTE = 10 * 60;
  private static final int DAY_END_MINUTE = 21 * 60;
  private static final double SPEED_KM_PER_HOUR = 15.0;
  private static final int TRANSFER_MINUTES = 10;
  private static final double EARTH_RADIUS_KM = 6371.0;

  /**
   * 추천 후보
   *
   * <ul>
   *   <li>baseScore: 아티스트·키워드 점수 합 (이벤트 점수 제외)
   *   <li>events: 여행 기간과 겹치는 이벤트
   *   <li>periodLimited: 운영 기간이 있는 장소 → 이벤트 기간에만 후보
   *   <li>openingHours: 요일별 영업시간. 비어 있으면 영업시간 없음 (언제든 방문 가능)
   * </ul>
   */
  public record Candidate(
      Place place,
      int baseScore,
      List<PlaceEvent> events,
      boolean periodLimited,
      Map<Weekday, PlaceOpeningHour> openingHours) {}

  public record Stop(Place place, LocalTime arrivalTime) {}

  // 날짜 오름차순, 장소가 없는 날은 빈 목록
  public Map<LocalDate, List<Stop>> recommend(
      List<Candidate> candidates, List<LocalDate> visitDates) {
    Map<LocalDate, List<Stop>> schedule = new LinkedHashMap<>();
    Set<UUID> usedPlaceIds = new HashSet<>();

    for (LocalDate date : visitDates.stream().sorted().toList()) {
      List<Candidate> picked =
          candidates.stream()
              .filter(candidate -> !usedPlaceIds.contains(candidate.place().getId()))
              .filter(candidate -> isAvailableOn(candidate, date))
              .sorted(
                  Comparator.comparingInt((Candidate candidate) -> score(candidate, date))
                      .reversed()
                      .thenComparing(candidate -> candidate.place().getId()))
              .limit(MAX_PLACES_PER_DAY)
              .toList();

      List<Stop> stops = assignArrivalTimes(orderByNearest(picked), date);
      stops.forEach(stop -> usedPlaceIds.add(stop.place().getId()));
      schedule.put(date, stops);
    }
    return schedule;
  }

  private boolean isAvailableOn(Candidate candidate, LocalDate date) {
    PlaceEvent event = ongoingEvent(candidate, date);
    if (candidate.periodLimited() && event == null) {
      return false;
    }
    // 이벤트 운영시간이 있으면 요일 휴무와 무관
    if (hasEventHours(event) || candidate.openingHours().isEmpty()) {
      return true;
    }
    PlaceOpeningHour hour = candidate.openingHours().get(Weekday.from(date.getDayOfWeek()));
    return hour != null && !hour.isClosed();
  }

  private int score(Candidate candidate, LocalDate date) {
    return candidate.baseScore() + (ongoingEvent(candidate, date) != null ? EVENT_BONUS : 0);
  }

  // 겹치는 이벤트가 여럿이면 가장 최근 시작한 이벤트
  private PlaceEvent ongoingEvent(Candidate candidate, LocalDate date) {
    return candidate.events().stream()
        .filter(event -> event.isOngoingOn(date))
        .max(Comparator.comparing(PlaceEvent::getStartsOn))
        .orElse(null);
  }

  private boolean hasEventHours(PlaceEvent event) {
    return event != null && event.getOpensAt() != null && event.getClosesAt() != null;
  }

  private List<Candidate> orderByNearest(List<Candidate> picked) {
    List<Candidate> remaining = new ArrayList<>(picked);
    List<Candidate> ordered = new ArrayList<>();
    if (remaining.isEmpty()) {
      return ordered;
    }
    Candidate current = remaining.removeFirst();
    ordered.add(current);
    while (!remaining.isEmpty()) {
      Place from = current.place();
      Candidate next =
          remaining.stream()
              .min(Comparator.comparingDouble(candidate -> distanceKm(from, candidate.place())))
              .orElseThrow();
      remaining.remove(next);
      ordered.add(next);
      current = next;
    }
    return ordered;
  }

  private List<Stop> assignArrivalTimes(List<Candidate> ordered, LocalDate date) {
    List<Stop> stops = new ArrayList<>();
    int minute = DAY_START_MINUTE;
    Place previous = null;

    for (Candidate candidate : ordered) {
      Place place = candidate.place();
      int arrival = previous == null ? minute : minute + travelMinutes(previous, place);
      if (arrival > DAY_END_MINUTE) {
        continue;
      }

      LocalTime visitTime = visitTime(candidate, date, LocalTime.of(arrival / 60, arrival % 60));
      if (visitTime == null) {
        continue;
      }
      int visitMinute = visitTime.getHour() * 60 + visitTime.getMinute();
      if (visitMinute > DAY_END_MINUTE) {
        continue;
      }

      stops.add(new Stop(place, visitTime));
      minute = visitMinute + place.getAvgStayMinutes();
      previous = place;
    }
    return stops;
  }

  // 방문 가능 시각. 영업시간 안에 방문 불가면 null
  private LocalTime visitTime(Candidate candidate, LocalDate date, LocalTime arrival) {
    PlaceEvent event = ongoingEvent(candidate, date);
    if (hasEventHours(event)) {
      return OpeningHours.earliestVisitTime(
          event.getOpensAt(), event.getClosesAt(), null, null, arrival);
    }
    // 영업시간 없음 → 언제든 방문 가능
    if (candidate.openingHours().isEmpty()) {
      return arrival;
    }
    PlaceOpeningHour hour = candidate.openingHours().get(Weekday.from(date.getDayOfWeek()));
    if (hour == null || hour.isClosed()) {
      return null;
    }
    return OpeningHours.earliestVisitTime(
        hour.getOpensAt(),
        hour.getClosesAt(),
        hour.getBreakStartsAt(),
        hour.getBreakEndsAt(),
        arrival);
  }

  private int travelMinutes(Place from, Place to) {
    return TRANSFER_MINUTES + (int) Math.ceil(distanceKm(from, to) / SPEED_KM_PER_HOUR * 60);
  }

  // 하버사인 직선거리 (Point 의 x = 경도, y = 위도)
  private double distanceKm(Place from, Place to) {
    double lat1 = Math.toRadians(from.getLocation().getY());
    double lat2 = Math.toRadians(to.getLocation().getY());
    double deltaLat = lat2 - lat1;
    double deltaLng = Math.toRadians(to.getLocation().getX() - from.getLocation().getX());
    double h =
        Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
            + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
    return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(h));
  }
}
