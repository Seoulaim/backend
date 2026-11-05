package com.seoulaim.domain.content.repository;

import com.seoulaim.domain.content.entity.Banner;
import com.seoulaim.domain.content.entity.BannerPlacement;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BannerRepository extends JpaRepository<Banner, UUID> {

  // 위치별 어드민 순서
  List<Banner> findAllByPlacementOrderByDisplayOrderAscIdAsc(BannerPlacement placement);

  // 위치 지정 없는 어드민 목록: 최신순 (어드민 mock getPromotions(null) 과 동일)
  List<Banner> findAllByOrderByCreatedAtDescIdDesc();

  // 기본 배너 전환·생성 시 그 위치 배너를 잠근다 (동시 전환으로 기본 배너가 둘이 되는 것 방지)
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT b FROM Banner b WHERE b.placement = :placement ORDER BY b.displayOrder ASC, b.id ASC")
  List<Banner> findAllByPlacementForUpdate(@Param("placement") BannerPlacement placement);

  @Query("SELECT b.placement FROM Banner b WHERE b.id = :id")
  Optional<BannerPlacement> findPlacementById(@Param("id") UUID id);

  @Query("SELECT COALESCE(MAX(b.displayOrder), -1) FROM Banner b WHERE b.placement = :placement")
  int findMaxDisplayOrder(@Param("placement") BannerPlacement placement);
}
