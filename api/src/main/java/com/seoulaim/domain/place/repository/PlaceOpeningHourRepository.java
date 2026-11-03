package com.seoulaim.domain.place.repository;

import com.seoulaim.domain.place.entity.PlaceOpeningHour;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceOpeningHourRepository extends JpaRepository<PlaceOpeningHour, UUID> {

  List<PlaceOpeningHour> findAllByPlaceId(UUID placeId);

  List<PlaceOpeningHour> findAllByPlaceIdIn(Collection<UUID> placeIds);

  @Modifying(flushAutomatically = true)
  @Query("DELETE FROM PlaceOpeningHour h WHERE h.place.id = :placeId")
  void deleteAllByPlaceId(@Param("placeId") UUID placeId);
}
