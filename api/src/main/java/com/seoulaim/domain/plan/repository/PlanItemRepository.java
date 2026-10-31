package com.seoulaim.domain.plan.repository;

import com.seoulaim.domain.plan.entity.PlanItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanItemRepository extends JpaRepository<PlanItem, UUID> {

  @Query(
      """
            SELECT i FROM PlanItem i
            JOIN FETCH i.place
            WHERE i.planDay.plan.id = :planId
            ORDER BY i.planDay.dayNumber ASC, i.sequence ASC
            """)
  List<PlanItem> findAllWithPlaceByPlanId(@Param("planId") UUID planId);

  @Modifying
  @Query(
      "DELETE FROM PlanItem i WHERE i.planDay.id IN "
          + "(SELECT d.id FROM PlanDay d WHERE d.plan.id = :planId)")
  void deleteAllByPlanId(@Param("planId") UUID planId);
}
