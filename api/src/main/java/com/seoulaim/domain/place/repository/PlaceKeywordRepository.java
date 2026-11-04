package com.seoulaim.domain.place.repository;

import com.seoulaim.domain.place.entity.PlaceKeyword;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceKeywordRepository extends JpaRepository<PlaceKeyword, UUID> {

  @Query("SELECT pk FROM PlaceKeyword pk JOIN FETCH pk.place WHERE pk.keyword.id IN :keywordIds")
  List<PlaceKeyword> findAllWithPlaceByKeywordIdIn(
      @Param("keywordIds") Collection<UUID> keywordIds);

  // 키워드 노출 순서대로
  @Query(
      """
                  SELECT pk FROM PlaceKeyword pk
                  JOIN FETCH pk.keyword k
                  WHERE pk.place.id IN :placeIds
                  ORDER BY k.displayOrder ASC, k.id ASC
                  """)
  List<PlaceKeyword> findAllWithKeywordByPlaceIdIn(@Param("placeIds") Collection<UUID> placeIds);

  // [장소 id, 키워드 수]
  @Query(
      "SELECT pk.place.id, COUNT(pk) FROM PlaceKeyword pk WHERE pk.place.id IN :placeIds"
          + " GROUP BY pk.place.id")
  List<Object[]> countByPlaceIds(@Param("placeIds") Collection<UUID> placeIds);

  // [키워드 id, 장소 수]
  @Query("SELECT pk.keyword.id, COUNT(pk) FROM PlaceKeyword pk GROUP BY pk.keyword.id")
  List<Object[]> countPlacesByKeyword();

  long countByKeywordId(UUID keywordId);

  @Modifying(flushAutomatically = true)
  @Query("DELETE FROM PlaceKeyword pk WHERE pk.place.id = :placeId")
  void deleteAllByPlaceId(@Param("placeId") UUID placeId);

  @Modifying(flushAutomatically = true)
  @Query("DELETE FROM PlaceKeyword pk WHERE pk.keyword.id = :keywordId")
  void deleteAllByKeywordId(@Param("keywordId") UUID keywordId);
}
