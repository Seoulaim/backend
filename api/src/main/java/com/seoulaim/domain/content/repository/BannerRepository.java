package com.seoulaim.domain.content.repository;

import com.seoulaim.domain.content.entity.Banner;
import com.seoulaim.domain.content.entity.BannerPlacement;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BannerRepository extends JpaRepository<Banner, UUID> {

  // 위치별 어드민 순서
  List<Banner> findAllByPlacementOrderByDisplayOrderAscIdAsc(BannerPlacement placement);

  // 위치 지정 없는 어드민 목록: 최신순 (어드민 mock getPromotions(null) 과 동일)
  List<Banner> findAllByOrderByCreatedAtDescIdDesc();

  @Query("SELECT COALESCE(MAX(b.displayOrder), -1) FROM Banner b WHERE b.placement = :placement")
  int findMaxDisplayOrder(@Param("placement") BannerPlacement placement);
}
