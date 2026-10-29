package com.seoulaim.domain.artist.repository;

import com.seoulaim.domain.artist.entity.Artist;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtistRepository extends JpaRepository<Artist, UUID> {

  List<Artist> findAllByArtistGroupIdOrderByDisplayOrderAscIdAsc(UUID artistGroupId);

  /**
   * 아티스트·그룹 이름 부분 일치 검색 (대소문자 무시)
   *
   * <ul>
   *   <li>ILIKE → name_en·name_ko trigram 인덱스 사용
   *   <li>pattern 은 %·_ 이스케이프 후 %keyword% 형태로 전달
   *   <li>cursor: 이전 페이지 마지막 id (첫 페이지는 null)
   * </ul>
   */
  @Query(
      value =
          """
                  SELECT a.*
                  FROM artists a
                  JOIN artist_groups g ON g.id = a.artist_group_id
                  WHERE (a.name_en ILIKE :pattern OR a.name_ko ILIKE :pattern
                         OR g.name_en ILIKE :pattern OR g.name_ko ILIKE :pattern)
                    AND (CAST(:cursor AS uuid) IS NULL OR a.id > CAST(:cursor AS uuid))
                  ORDER BY a.id
                  LIMIT :limit
                  """,
      nativeQuery = true)
  List<Artist> searchByKeyword(
      @Param("pattern") String pattern, @Param("cursor") String cursor, @Param("limit") int limit);

  // bias 저장 시 존재 확인 + 응답용 그룹 정보 함께 조회
  @Query("SELECT a FROM Artist a JOIN FETCH a.artistGroup WHERE a.id IN :ids")
  List<Artist> findAllWithGroupByIdIn(@Param("ids") Collection<UUID> ids);
}
