package com.seoulaim.domain.plan.repository;

import com.seoulaim.domain.plan.entity.PlanArtist;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanArtistRepository extends JpaRepository<PlanArtist, UUID> {

  // 요청 순서대로, 그룹 정보 함께 조회
  @Query(
      """
                    SELECT pa FROM PlanArtist pa
                    JOIN FETCH pa.artist a
                    JOIN FETCH a.artistGroup
                    WHERE pa.plan.id = :planId
                    ORDER BY pa.displayOrder ASC
                    """)
  List<PlanArtist> findAllWithArtistByPlanId(@Param("planId") UUID planId);

  @Modifying(flushAutomatically = true)
  @Query("DELETE FROM PlanArtist pa WHERE pa.plan.id = :planId")
  void deleteAllByPlanId(@Param("planId") UUID planId);
}
