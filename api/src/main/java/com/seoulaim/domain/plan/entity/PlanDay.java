package com.seoulaim.domain.plan.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 일정이 있는 하루 - 체류 기간 중 투어 날짜를 비연속으로 선택 가능 → 시작일 기준 계산 대신 날짜 직접 저장 - 투어 일수 = 이 테이블의 row 수 */
@Getter
@Entity
@Table(
    name = "plan_days",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_plan_days_plan_number",
          columnNames = {"plan_id", "day_number"}),
      @UniqueConstraint(
          name = "uk_plan_days_plan_date",
          columnNames = {"plan_id", "visit_date"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanDay extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_id", nullable = false)
  private TravelPlan plan;

  // 1부터 시작, visitDate 오름차순과 일치 (DAY 1, DAY 2 ...)
  @Column(name = "day_number", nullable = false)
  private int dayNumber;

  @Column(name = "visit_date", nullable = false)
  private LocalDate visitDate;

  public static PlanDay create(TravelPlan plan, int dayNumber, LocalDate visitDate) {
    PlanDay day = new PlanDay();
    day.plan = plan;
    day.dayNumber = dayNumber;
    day.visitDate = visitDate;
    return day;
  }
}
