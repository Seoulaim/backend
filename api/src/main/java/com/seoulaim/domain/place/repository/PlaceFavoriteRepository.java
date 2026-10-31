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
}
