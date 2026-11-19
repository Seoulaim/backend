package com.seoulaim.domain.plan.repository;

import com.seoulaim.domain.plan.entity.TravelPlan;
import com.seoulaim.domain.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, UUID> {

  // 다른 유저의 일정은 없는 것과 동일하게 처리
  Optional<TravelPlan> findByIdAndUserId(UUID id, UUID userId);

  // UUID v7 → id 내림차순 = 최신 생성순
  List<TravelPlan> findAllByUserIdOrderByIdDesc(UUID userId, Limit limit);

  List<TravelPlan> findAllByUserIdAndIdLessThanOrderByIdDesc(UUID userId, UUID cursor, Limit limit);

  boolean existsByUserId(UUID userId);

  long countByUserId(UUID userId);

  List<TravelPlan> findAllByUserId(UUID userId);

  // UUID v7 → id 가 가장 큰 일정 = 가장 최근에 만든 일정
  Optional<TravelPlan> findFirstByUserIdOrderByIdDesc(UUID userId);

  // 일정 생성 동시 요청 직렬화용. 사용자 행 잠금 (트랜잭션 종료 시 해제)
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM User u WHERE u.id = :userId")
  Optional<User> findUserForUpdate(@Param("userId") UUID userId);

  // 일정을 하나 이상 가진 사용자 수
  @Query("SELECT COUNT(DISTINCT p.user.id) FROM TravelPlan p")
  long countDistinctUsers();
}
