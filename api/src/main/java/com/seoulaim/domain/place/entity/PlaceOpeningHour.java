package com.seoulaim.domain.place.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장소의 요일별 영업시간
 *
 * <ul>
 *   <li>장소당 요일별 1행 (7행). 행이 없으면 "영업시간 없음 (언제든 방문 가능)"
 *   <li>휴무일은 시각 모두 null
 *   <li>브레이크는 하루 1개
 * </ul>
 */
@Getter
@Entity
@Table(
    name = "place_opening_hours",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_place_opening_hours_place_day",
            columnNames = {"place_id", "day_of_week"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceOpeningHour extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "place_id", nullable = false)
  private Place place;

  @Enumerated(EnumType.STRING)
  @Column(name = "day_of_week", nullable = false, length = 3)
  private Weekday dayOfWeek;

  @Column(name = "closed", nullable = false)
  private boolean closed;

  @Column(name = "opens_at")
  private LocalTime opensAt;

  @Column(name = "closes_at")
  private LocalTime closesAt;

  @Column(name = "break_starts_at")
  private LocalTime breakStartsAt;

  @Column(name = "break_ends_at")
  private LocalTime breakEndsAt;

  public static PlaceOpeningHour open(
      Place place,
      Weekday dayOfWeek,
      LocalTime opensAt,
      LocalTime closesAt,
      LocalTime breakStartsAt,
      LocalTime breakEndsAt) {
    PlaceOpeningHour hour = new PlaceOpeningHour();
    hour.place = place;
    hour.dayOfWeek = dayOfWeek;
    hour.closed = false;
    hour.opensAt = opensAt;
    hour.closesAt = closesAt;
    hour.breakStartsAt = breakStartsAt;
    hour.breakEndsAt = breakEndsAt;
    return hour;
  }

  public static PlaceOpeningHour closedOn(Place place, Weekday dayOfWeek) {
    PlaceOpeningHour hour = new PlaceOpeningHour();
    hour.place = place;
    hour.dayOfWeek = dayOfWeek;
    hour.closed = true;
    return hour;
  }
}
