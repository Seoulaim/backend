package com.seoulaim.domain.place.repository;

import com.seoulaim.domain.place.entity.ArtistPlace;
import java.util.Collection;
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

  // 일정 후보: 멤버 직접 연결 + 그룹 단위 연결
  @Query(
      """
          SELECT ap FROM ArtistPlace ap
          JOIN FETCH ap.place
          JOIN FETCH ap.artistGroup
          LEFT JOIN FETCH ap.artist a
          WHERE a.id IN :artistIds OR (ap.artist IS NULL AND ap.artistGroup.id IN :groupIds)
          """)
  List<ArtistPlace> findAllByArtistsOrGroups(
      @Param("artistIds") Collection<UUID> artistIds, @Param("groupIds") Collection<UUID> groupIds);

  @Query(
      """
          SELECT ap FROM ArtistPlace ap
          JOIN FETCH ap.artistGroup
          LEFT JOIN FETCH ap.artist
          WHERE ap.place.id IN :placeIds
          ORDER BY ap.createdAt ASC, ap.id ASC
          """)
  List<ArtistPlace> findAllWithArtistByPlaceIdIn(@Param("placeIds") Collection<UUID> placeIds);
}
