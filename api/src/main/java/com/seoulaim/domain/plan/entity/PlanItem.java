package com.seoulaim.domain.plan.entity;

import com.seoulaim.domain.place.entity.Place;
import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 하루 일정의 방문 장소 하나 - (plan_day_id, sequence) unique 미설정: 순서 변경 중 일시적 중복으로 제약 위반 발생 - 순서 중복은 서비스에서 하루
 * 전체 순서를 한 트랜잭션으로 재계산해 방지
 */
@Getter
@Entity
@Table(
    name = "plan_items",
    indexes = {
      @Index(name = "idx_plan_items_day_sequence", columnList = "plan_day_id, sequence"),
      @Index(name = "idx_plan_items_place_id", columnList = "place_id")
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanItem extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_day_id", nullable = false)
  private PlanDay planDay;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "place_id", nullable = false)
  private Place place;

  // 하루 안의 방문 순서. 지도 경로도 이 순서로 연결
  @Column(name = "sequence", nullable = false)
  private int sequence;

  // 추천 알고리즘이 이동·체류 시간으로 계산한 예상 도착 시각
  @Column(name = "arrival_time")
  private LocalTime arrivalTime;

  public static PlanItem create(PlanDay planDay, Place place, int sequence, LocalTime arrivalTime) {
    PlanItem item = new PlanItem();
    item.planDay = planDay;
    item.place = place;
    item.sequence = sequence;
    item.arrivalTime = arrivalTime;
    return item;
  }

  public void changeSequence(int sequence) {
    this.sequence = sequence;
  }
}
