package com.seoulaim.domain.plan.repository;

import com.seoulaim.domain.plan.entity.PlanDay;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanDayRepository extends JpaRepository<PlanDay, UUID> {

  List<PlanDay> findAllByPlanIdOrderByDayNumberAsc(UUID planId);

  // [planId, dayCount]
  @Query("SELECT d.plan.id, COUNT(d) FROM PlanDay d WHERE d.plan.id IN :planIds GROUP BY d.plan.id")
  List<Object[]> countByPlanIds(@Param("planIds") Collection<UUID> planIds);

  @Modifying
  @Query("DELETE FROM PlanDay d WHERE d.plan.id = :planId")
  void deleteAllByPlanId(@Param("planId") UUID planId);
}
