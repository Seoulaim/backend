package com.seoulaim.domain.plan.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.seoulaim.domain.place.entity.Place;
import com.seoulaim.domain.place.entity.PlaceCategory;
import com.seoulaim.domain.plan.service.PlanRecommender.Candidate;
import com.seoulaim.domain.plan.service.PlanRecommender.Stop;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

class PlanRecommenderTest {

  private static final GeometryFactory GEOMETRY = new GeometryFactory(new PrecisionModel(), 4326);
  private static final List<LocalDate> DATES =
      List.of(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 2), LocalDate.of(2026, 12, 3));
  // 성수·홍대·강남
  private static final String[] AREAS = {"SEONGSU", "HONGDAE", "GANGNAM"};
  private static final double[][] CENTERS = {
    {37.544, 127.056}, {37.556, 126.923}, {37.498, 127.028}
  };

  private final PlanRecommender recommender = new PlanRecommender();
  private final Map<UUID, Candidate> byId = new HashMap<>();
  private final Map<UUID, String> areaById = new HashMap<>();

  @Test
  @DisplayName("3개 지역 후보로 3일 일정 → 하루 한 지역, 종류별 개수 제한, 연속·순서 규칙, 중복 없음")
  void recommend_규칙() {
    List<Candidate> candidates = new ArrayList<>();
    for (int i = 0; i < AREAS.length; i++) {
      double lat = CENTERS[i][0];
      double lng = CENTERS[i][1];
      String area = AREAS[i];
      for (int n = 0; n < 4; n++) {
        candidates.add(candidate(area, lat + 0.001 * (n + 1), lng, 5, PlaceKind.RESTAURANT, false));
      }
      for (int n = 0; n < 3; n++) {
        candidates.add(candidate(area, lat, lng + 0.001 * (n + 1), 5, PlaceKind.CAFE, false));
      }
      candidates.add(candidate(area, lat - 0.001, lng, 6, PlaceKind.BAR, i == 0));
      candidates.add(candidate(area, lat - 0.002, lng + 0.002, 3, PlaceKind.OTHER, false));
      candidates.add(candidate(area, lat - 0.003, lng - 0.001, 2, PlaceKind.OTHER, false));
    }
    candidates.add(candidate("SEONGSU", 37.545, 127.057, 8, PlaceKind.RESTAURANT, true));

    Map<LocalDate, List<Stop>> schedule = recommender.recommend(candidates, DATES);

    Set<UUID> seen = new HashSet<>();
    for (List<Stop> stops : schedule.values()) {
      List<Candidate> day = stops.stream().map(stop -> byId.get(stop.place().getId())).toList();
      assertThat(day).hasSize(5);
      // 하루 한 지역
      assertThat(day.stream().map(c -> areaById.get(c.place().getId())).distinct()).hasSize(1);
      // 종류별 개수 제한
      assertThat(count(day, PlaceKind.RESTAURANT)).isLessThanOrEqualTo(3);
      assertThat(count(day, PlaceKind.CAFE)).isLessThanOrEqualTo(2);
      assertThat(count(day, PlaceKind.OTHER)).isGreaterThanOrEqualTo(2);
      assertThat(day.stream().filter(Candidate::premium).count()).isLessThanOrEqualTo(1);
      for (int i = 1; i < day.size(); i++) {
        PlaceKind previous = day.get(i - 1).kind();
        PlaceKind current = day.get(i).kind();
        // 같은 종류 연속 금지 (기타 장소 제외)
        if (current != PlaceKind.OTHER) {
          assertThat(current).isNotEqualTo(previous);
        }
        // 술집 뒤에는 술집만
        if (previous == PlaceKind.BAR) {
          assertThat(current).isEqualTo(PlaceKind.BAR);
        }
      }
      // 중복 없음
      day.forEach(c -> assertThat(seen.add(c.place().getId())).isTrue());
    }
  }

  @Test
  @DisplayName("키워드로 종류·고가 판단: BAR > RESTAURANT > CAFE, BIRTHDAY_CAFE 는 기타")
  void placeKind_키워드() {
    assertThat(PlaceKind.from(Set.of("CAFE", "RESTAURANT"))).isEqualTo(PlaceKind.RESTAURANT);
    assertThat(PlaceKind.from(Set.of("BAR", "RESTAURANT"))).isEqualTo(PlaceKind.BAR);
    assertThat(PlaceKind.from(Set.of("BIRTHDAY_CAFE"))).isEqualTo(PlaceKind.OTHER);
    assertThat(PlaceKind.isPremium(Set.of("PREMIUM", "CAFE"))).isTrue();
  }

  private Candidate candidate(
      String area, double lat, double lng, int score, PlaceKind kind, boolean premium) {
    Place place =
        Place.create(
            area + "-" + kind + "-" + byId.size(),
            PlaceCategory.FANDOM_PLACE,
            "서울",
            GEOMETRY.createPoint(new Coordinate(lng, lat)),
            null,
            null,
            null);
    Candidate candidate = new Candidate(place, score, List.of(), false, Map.of(), kind, premium);
    byId.put(place.getId(), candidate);
    areaById.put(place.getId(), area);
    return candidate;
  }

  private static long count(List<Candidate> day, PlaceKind kind) {
    return day.stream().filter(c -> c.kind() == kind).count();
  }
}
