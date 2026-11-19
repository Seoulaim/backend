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

  List<Artist> findAllByArtistGroupIdInOrderByDisplayOrderAscIdAsc(Collection<UUID> artistGroupIds);

  /**
   * 아티스트·그룹 이름 검색 (4개 언어, 대소문자·공백·하이픈 무시)
   *
   * <ul>
   *   <li>pattern: 소문자, 공백·하이픈·| 제거, %·_ 이스케이프 후 %keyword% 형태
   *   <li>이름은 언어별로 | 로 이어 붙여 비교 → 언어 경계를 넘는 일치 없음
   *   <li>정렬: 멤버 이름 일치 → 그룹 이름만 일치, 그다음 그룹·멤버 노출 순서
   *   <li>offset 페이징 (cursor = 다음 offset)
   * </ul>
   */
  @Query(
      value =
          """
                                  SELECT a.*
                                  FROM artists a
                                  JOIN artist_groups g ON g.id = a.artist_group_id
                                  WHERE translate(lower(concat_ws('|', a.name_en, a.name_ko, a.name_ja, a.name_zh)),
                                                  ' -', '') LIKE :pattern
                                     OR translate(lower(concat_ws('|', g.name_en, g.name_ko, g.name_ja, g.name_zh)),
                                                  ' -', '') LIKE :pattern
                                  ORDER BY CASE
                                               WHEN translate(lower(concat_ws('|', a.name_en, a.name_ko, a.name_ja,
                                                                              a.name_zh)), ' -', '') LIKE :pattern
                                                   THEN 0
                                               ELSE 1
                                           END,
                                           g.display_order, g.id, a.display_order, a.id
                                  OFFSET :offset
                                  LIMIT :limit
                                  """,
      nativeQuery = true)
  List<Artist> searchByKeyword(
      @Param("pattern") String pattern, @Param("offset") int offset, @Param("limit") int limit);

  // bias 저장 시 존재 확인 + 응답용 그룹 정보 함께 조회
  @Query("SELECT a FROM Artist a JOIN FETCH a.artistGroup WHERE a.id IN :ids")
  List<Artist> findAllWithGroupByIdIn(@Param("ids") Collection<UUID> ids);
}
