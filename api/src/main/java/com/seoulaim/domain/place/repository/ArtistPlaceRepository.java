package com.seoulaim.domain.place.repository;

import com.seoulaim.domain.place.entity.ArtistPlace;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtistPlaceRepository extends JpaRepository<ArtistPlace, UUID> {

  // 그룹 단위 연결은 artist 가 null → LEFT JOIN
  @Query(
      """
            SELECT ap FROM ArtistPlace ap
            JOIN FETCH ap.artistGroup
            LEFT JOIN FETCH ap.artist
            WHERE ap.place.id = :placeId
            ORDER BY ap.createdAt ASC, ap.id ASC
            """)
  List<ArtistPlace> findAllWithArtistByPlaceId(@Param("placeId") UUID placeId);
}
