package com.seoulaim.domain.goods.repository;

import com.seoulaim.domain.goods.entity.Promotion;
import com.seoulaim.domain.goods.entity.PromotionImportance;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

  // starts_at/ends_at 이 null 이면 해당 방향으로 기간 제한이 없는 것으로 본다
  @Query(
      """
      select p from Promotion p
      where p.importance = :importance
        and (p.startsAt is null or p.startsAt <= :now)
        and (p.endsAt is null or p.endsAt > :now)
      order by p.displayOrder asc nulls last, p.id asc
      """)
  List<Promotion> findAllActive(
      @Param("importance") PromotionImportance importance, @Param("now") Instant now);
}
