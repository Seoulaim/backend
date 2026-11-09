package com.seoulaim.domain.diary.repository;

import com.seoulaim.domain.diary.entity.DiaryStickerCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiaryStickerCategoryRepository extends JpaRepository<DiaryStickerCategory, UUID> {

  List<DiaryStickerCategory> findAllByOrderByDisplayOrderAscIdAsc();

  Optional<DiaryStickerCategory> findTopByOrderByDisplayOrderDesc();

  /** 활성 스티커만 센다 (어드민 화면은 비활성 스티커를 삭제된 것으로 본다). 스티커가 없는 카테고리는 빠진다. */
  @Query(
      value =
          """
          SELECT m.category_id AS categoryId, count(*) AS count
          FROM diary_sticker_category_map m
          JOIN diary_stickers s ON s.id = m.sticker_id
          WHERE s.is_active
          GROUP BY m.category_id
          """,
      nativeQuery = true)
  List<CategoryStickerCount> countActiveStickersByCategory();

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      value = "DELETE FROM diary_sticker_category_map WHERE category_id = :categoryId",
      nativeQuery = true)
  int deleteMappingsByCategoryId(@Param("categoryId") UUID categoryId);
}
