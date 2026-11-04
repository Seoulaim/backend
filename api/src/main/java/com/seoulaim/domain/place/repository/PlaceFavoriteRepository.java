package com.seoulaim.domain.place.repository;

import com.seoulaim.domain.place.entity.PlaceFavorite;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceFavoriteRepository extends JpaRepository<PlaceFavorite, UUID> {

  boolean existsByUserIdAndPlaceId(UUID userId, UUID placeId);

  @Modifying
  @Query("DELETE FROM PlaceFavorite f WHERE f.user.id = :userId AND f.place.id = :placeId")
  void deleteByUserIdAndPlaceId(@Param("userId") UUID userId, @Param("placeId") UUID placeId);

  // 회원 탈퇴용. 벌크 delete → 영속성 컨텍스트 비움
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("DELETE FROM PlaceFavorite f WHERE f.user.id = :userId")
  void deleteAllByUserId(@Param("userId") UUID userId);

  @Modifying(flushAutomatically = true)
  @Query("DELETE FROM PlaceFavorite f WHERE f.place.id = :placeId")
  void deleteAllByPlaceId(@Param("placeId") UUID placeId);
}
