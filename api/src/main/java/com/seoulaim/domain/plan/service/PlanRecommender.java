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
 * 일정 추천 알고리즘
 *
 * <p>점수
 *
 * <ul>
 *   <li>멤버 연결 3, 그룹 연결 2, 키워드 일치 3, 당일 이벤트 +3
 *   <li>운영 기간이 있는 장소는 기간 안의 날짜에만 후보, 요일 휴무면 그날 후보 제외
 * </ul>
 *
 * <p>날짜별 장소 선택 (최대 5곳, 이미 배정한 장소는 다른 날 제외)
 *
 * <ul>
 *   <li>후보마다 기준 장소로 하루를 구성해 보고 "점수 - 선택된 장소들 중심까지 거리 × 감점" 합이 가장 큰 구성 선택 → 하루 동선을 한 지역에 모음
 *   <li>식당 최대 3, 카페 최대 2, 술집 최대 1, 고가(PREMIUM) 최대 1
 *   <li>식당·카페·술집 외 장소 최소 2곳 (후보가 부족하면 있는 만큼)
 * </ul>
 *
 * <p>방문 순서
 *
 * <ul>
 *   <li>식당·카페·술집은 같은 종류 연속 금지, 술집은 맨 뒤
 *   <li>조건을 지키는 순서 중 총 이동 거리가 가장 짧은 순서 (최대 5곳 → 전체 순열 비교)
 *   <li>조건을 지키는 순서가 없으면 거리만 기준
 * </ul>
 *
 * <p>방문 시각: 직선거리 ÷ 15km/h + 환승 10분, 10:00 출발 ~ 21:00. 여는 시각 전·브레이크 중 도착 → 영업 재개까지 대기
 */
@Component
public class PlanRecommender {

  static final int ARTIST_SCORE = 3;
  static final int GROUP_SCORE = 2;
  static final int KEYWORD_SCORE = 3;
  private static final int EVENT_BONUS = 3;

  private static final int MAX_PLACES_PER_DAY = 5;
  private static final Map<PlaceKind, Integer> MAX_PER_KIND =
      Map.of(PlaceKind.RESTAURANT, 3, PlaceKind.CAFE, 2, PlaceKind.BAR, 1);
  private static final int MAX_PREMIUM_PER_DAY = 1;
  private static final int MIN_OTHER_PER_DAY = 2;
  // 중심에서 1km 멀어질 때마다 0.5점 감점 (10km ≈ 멤버 연결 + 그룹 연결)
  private static final double DISTANCE_PENALTY_PER_KM = 0.5;

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
   *   <li>kind·premium: 장소 키워드로 판단한 종류·고가 여부
   * </ul>
   */
  public record Candidate(
      Place place,
      int baseScore,
      List<PlaceEvent> events,
      boolean periodLimited,
      Map<Weekday, PlaceOpeningHour> openingHours,
      PlaceKind kind,
      boolean premium) {}

  public record Stop(Place place, LocalTime arrivalTime) {}

  // 날짜 오름차순, 장소가 없는 날은 빈 목록
  public Map<LocalDate, List<Stop>> recommend(
      List<Candidate> candidates, List<LocalDate> visitDates) {
    Map<LocalDate, List<Stop>> schedule = new LinkedHashMap<>();
    Set<UUID> usedPlaceIds = new HashSet<>();

    for (LocalDate date : visitDates.stream().sorted().toList()) {
      List<Candidate> available =
          candidates.stream()
              .filter(candidate -> !usedPlaceIds.contains(candidate.place().getId()))
              .filter(candidate -> isAvailableOn(candidate, date))
              .sorted(
                  Comparator.comparingInt((Candidate candidate) -> score(candidate, date))
                      .reversed()
                      .thenComparing(candidate -> candidate.place().getId()))
              .toList();

      List<Candidate> picked = pickForDay(available, date);
      List<Stop> stops = assignArrivalTimes(order(picked), date);
      stops.forEach(stop -> usedPlaceIds.add(stop.place().getId()));
      schedule.put(date, stops);
    }
    return schedule;
  }

  // ---------- 날짜별 선택 ----------

  private record DayPick(List<Candidate> picked, double value) {}

  // 후보마다 기준 장소로 하루를 구성해 보고 가치 합이 가장 큰 구성. available 은 점수 내림차순
  private List<Candidate> pickForDay(List<Candidate> available, LocalDate date) {
    DayPick best = new DayPick(List.of(), Double.NEGATIVE_INFINITY);
    for (Candidate seed : available) {
      DayPick pick = pickFrom(seed, available, date);
      // 같은 값이면 앞선(점수 높은) 기준 장소 유지
      if (pick.value() > best.value()) {
        best = pick;
      }
    }
    return best.picked();
  }

  // 기준 장소에서 시작해 "점수 - 거리 감점" 이 높은 순으로 채움. 가치 = 고른 장소들의 (점수 - 감점) 합
  private DayPick pickFrom(Candidate seed, List<Candidate> available, LocalDate date) {
    List<Candidate> picked = new ArrayList<>(List.of(seed));
    List<Candidate> remaining = new ArrayList<>(available);
    remaining.remove(seed);
    double total = score(seed, date);
    while (picked.size() < MAX_PLACES_PER_DAY && !remaining.isEmpty()) {
      Candidate next = null;
      if (mustPickOther(picked, remaining)) {
        next = best(picked, remaining, date, true);
      }
      // 남은 기타 장소가 제한(고가 등)에 걸리면 종류 무관
      if (next == null) {
        next = best(picked, remaining, date, false);
      }
      if (next == null) {
        break;
      }
      total += score(next, date) - distancePenalty(picked, next);
      picked.add(next);
      remaining.remove(next);
    }
    return new DayPick(picked, total);
  }

  // 점수 - 거리 감점이 가장 높은 후보. 같은 값이면 앞선(점수 높은·id 작은) 후보
  private Candidate best(
      List<Candidate> picked, List<Candidate> remaining, LocalDate date, boolean otherOnly) {
    Candidate best = null;
    double bestValue = Double.NEGATIVE_INFINITY;
    for (Candidate candidate : remaining) {
      if (!fitsLimits(picked, candidate) || (otherOnly && candidate.kind().isFood())) {
        continue;
      }
      double value = score(candidate, date) - distancePenalty(picked, candidate);
      if (value > bestValue) {
        bestValue = value;
        best = candidate;
      }
    }
    return best;
  }

  // 남은 자리를 식당·카페·술집 외 장소 최소 개수 채우는 데 써야 하는지
  private boolean mustPickOther(List<Candidate> picked, List<Candidate> remaining) {
    long pickedOther = picked.stream().filter(candidate -> !candidate.kind().isFood()).count();
    long availableOther =
        remaining.stream().filter(candidate -> !candidate.kind().isFood()).count();
    long stillNeeded = Math.min(MIN_OTHER_PER_DAY - pickedOther, availableOther);
    return stillNeeded > 0 && MAX_PLACES_PER_DAY - picked.size() <= stillNeeded;
  }

  private boolean fitsLimits(List<Candidate> picked, Candidate candidate) {
    Integer maxOfKind = MAX_PER_KIND.get(candidate.kind());
    if (maxOfKind != null
        && picked.stream().filter(other -> other.kind() == candidate.kind()).count() >= maxOfKind) {
      return false;
    }
    return !candidate.premium()
        || picked.stream().filter(Candidate::premium).count() < MAX_PREMIUM_PER_DAY;
  }

  // 이미 고른 장소들의 중심까지 거리 기준 감점. 첫 장소는 감점 없음
  private double distancePenalty(List<Candidate> picked, Candidate candidate) {
    if (picked.isEmpty()) {
      return 0;
    }
    double latitude =
        picked.stream().mapToDouble(c -> c.place().getLocation().getY()).average().orElseThrow();
    double longitude =
        picked.stream().mapToDouble(c -> c.place().getLocation().getX()).average().orElseThrow();
    Place place = candidate.place();
    return DISTANCE_PENALTY_PER_KM
        * distanceKm(latitude, longitude, place.getLocation().getY(), place.getLocation().getX());
  }

  // ---------- 방문 순서 ----------

  // 조건(같은 종류 연속 금지, 술집 맨 뒤)을 지키는 순서 중 총 이동 거리 최소. 없으면 거리만 기준
  private List<Candidate> order(List<Candidate> picked) {
    List<List<Candidate>> permutations = new ArrayList<>();
    permute(new ArrayList<>(picked), 0, permutations);

    List<Candidate> best = null;
    double bestDistance = Double.MAX_VALUE;
    List<Candidate> shortest = picked;
    double shortestDistance = Double.MAX_VALUE;
    for (List<Candidate> permutation : permutations) {
      double distance = totalDistance(permutation);
      if (distance < shortestDistance) {
        shortestDistance = distance;
        shortest = permutation;
      }
      if (isValidOrder(permutation) && distance < bestDistance) {
        bestDistance = distance;
        best = permutation;
      }
    }
    return best != null ? best : shortest;
  }

  private void permute(List<Candidate> items, int index, List<List<Candidate>> result) {
    if (index == items.size()) {
      result.add(List.copyOf(items));
      return;
    }
    for (int i = index; i < items.size(); i++) {
      swap(items, index, i);
      permute(items, index + 1, result);
      swap(items, index, i);
    }
  }

  private static void swap(List<Candidate> items, int a, int b) {
    Candidate temp = items.get(a);
    items.set(a, items.get(b));
    items.set(b, temp);
  }

  private boolean isValidOrder(List<Candidate> ordered) {
    boolean barSeen = false;
    PlaceKind previous = null;
    for (Candidate candidate : ordered) {
      PlaceKind kind = candidate.kind();
      if (barSeen && kind != PlaceKind.BAR) {
        return false;
      }
      if (kind.isFood() && kind == previous) {
        return false;
      }
      barSeen |= kind == PlaceKind.BAR;
      previous = kind;
    }
    return true;
  }

  private double totalDistance(List<Candidate> ordered) {
    double total = 0;
    for (int i = 1; i < ordered.size(); i++) {
      total += distanceKm(ordered.get(i - 1).place(), ordered.get(i).place());
    }
    return total;
  }

  // ---------- 후보 판단 ----------

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

  // ---------- 방문 시각 ----------

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

  // Point 의 x = 경도, y = 위도
  private double distanceKm(Place from, Place to) {
    return distanceKm(
        from.getLocation().getY(),
        from.getLocation().getX(),
        to.getLocation().getY(),
        to.getLocation().getX());
  }

  // 하버사인 직선거리
  private double distanceKm(double lat1Deg, double lng1Deg, double lat2Deg, double lng2Deg) {
    double lat1 = Math.toRadians(lat1Deg);
    double lat2 = Math.toRadians(lat2Deg);
    double deltaLat = lat2 - lat1;
    double deltaLng = Math.toRadians(lng2Deg - lng1Deg);
    double h =
        Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
            + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
    return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(h));
  }
}
