package com.seoulaim.domain.place.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장소의 기간 한정 이벤트 (생일카페, 광고 게시 기간 등) - 한 장소에서 시기를 달리해 여러 번 개최 가능 - 추천 시 여행 날짜에 진행 중인 이벤트로 날짜별 후보 장소
 * 결정
 */
@Getter
@Entity
@Table(
    name = "place_events",
    indexes = {
      @Index(name = "idx_place_events_place_id", columnList = "place_id"),
      @Index(name = "idx_place_events_period", columnList = "starts_on, ends_on")
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceEvent extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "place_id", nullable = false)
  private Place place;

  @Column(name = "title", length = 200)
  private String title;

  @Column(name = "starts_on", nullable = false)
  private LocalDate startsOn;

  @Column(name = "ends_on", nullable = false)
  private LocalDate endsOn;

  // 이벤트 기간 운영시간. null 이면 장소의 상시 영업시간 사용
  @Column(name = "opens_at")
  private LocalTime opensAt;

  @Column(name = "closes_at")
  private LocalTime closesAt;

  public static PlaceEvent create(
      Place place,
      String title,
      LocalDate startsOn,
      LocalDate endsOn,
      LocalTime opensAt,
      LocalTime closesAt) {
    PlaceEvent event = new PlaceEvent();
    event.place = place;
    event.title = title;
    event.startsOn = startsOn;
    event.endsOn = endsOn;
    event.opensAt = opensAt;
    event.closesAt = closesAt;
    return event;
  }

  // 시작일·종료일 당일 포함
  public boolean isOngoingOn(LocalDate date) {
    return !date.isBefore(startsOn) && !date.isAfter(endsOn);
  }
}
