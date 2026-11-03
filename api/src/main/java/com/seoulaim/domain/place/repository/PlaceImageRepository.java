package com.seoulaim.domain.place.repository;

import com.seoulaim.domain.place.entity.PlaceImage;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceImageRepository extends JpaRepository<PlaceImage, UUID> {

  // 첫 번째가 대표 이미지
  List<PlaceImage> findAllByPlaceIdOrderByDisplayOrderAscIdAsc(UUID placeId);

  List<PlaceImage> findAllByPlaceIdInOrderByDisplayOrderAscIdAsc(Collection<UUID> placeIds);

  @Modifying(flushAutomatically = true)
  @Query("DELETE FROM PlaceImage i WHERE i.place.id = :placeId")
  void deleteAllByPlaceId(@Param("placeId") UUID placeId);
}
