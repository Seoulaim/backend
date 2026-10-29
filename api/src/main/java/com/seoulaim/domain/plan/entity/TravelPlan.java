package com.seoulaim.domain.plan.entity;

import com.seoulaim.domain.user.entity.User;
import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 여행 일정 (TravelPlan → PlanDay → PlanItem) - startDate ~ endDate: 한국 체류 전체 기간 - 실제 투어하는 날은 그 중 일부 →
 * PlanDay 에 별도 저장 - 제목은 닉네임으로 생성해 표시 → 저장 안 함 - 사용자당 보유 개수 제한은 요금제별로 서비스에서 검증
 */
@Getter
@Entity
@Table(
    name = "travel_plans",
    indexes = @Index(name = "idx_travel_plans_user_created", columnList = "user_id, created_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelPlan extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  // 기간 역전·최대 기간 검증은 Request DTO, 서비스에서 처리
  public static TravelPlan create(User user, LocalDate startDate, LocalDate endDate) {
    TravelPlan plan = new TravelPlan();
    plan.user = user;
    plan.startDate = startDate;
    plan.endDate = endDate;
    return plan;
  }

  public boolean isOwnedBy(UUID userId) {
    return user.getId().equals(userId);
  }
}
