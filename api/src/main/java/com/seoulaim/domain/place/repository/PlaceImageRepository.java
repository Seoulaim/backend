package com.seoulaim.domain.place.repository;

import com.seoulaim.domain.place.entity.PlaceImage;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceImageRepository extends JpaRepository<PlaceImage, UUID> {

  // 첫 번째가 대표 이미지
  List<PlaceImage> findAllByPlaceIdOrderByDisplayOrderAscIdAsc(UUID placeId);

  List<PlaceImage> findAllByPlaceIdInOrderByDisplayOrderAscIdAsc(Collection<UUID> placeIds);
}
