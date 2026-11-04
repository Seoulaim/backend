package com.seoulaim.domain.plan.repository;

import com.seoulaim.domain.plan.entity.TravelPlan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, UUID> {

  // 다른 유저의 일정은 없는 것과 동일하게 처리
  Optional<TravelPlan> findByIdAndUserId(UUID id, UUID userId);

  // UUID v7 → id 내림차순 = 최신 생성순
  List<TravelPlan> findAllByUserIdOrderByIdDesc(UUID userId, Limit limit);

  List<TravelPlan> findAllByUserIdAndIdLessThanOrderByIdDesc(UUID userId, UUID cursor, Limit limit);

  boolean existsByUserId(UUID userId);

  List<TravelPlan> findAllByUserId(UUID userId);

  // UUID v7 → id 가 가장 큰 일정 = 가장 최근에 만든 일정
  Optional<TravelPlan> findFirstByUserIdOrderByIdDesc(UUID userId);

  // 일정을 하나 이상 가진 사용자 수
  @Query("SELECT COUNT(DISTINCT p.user.id) FROM TravelPlan p")
  long countDistinctUsers();
}
