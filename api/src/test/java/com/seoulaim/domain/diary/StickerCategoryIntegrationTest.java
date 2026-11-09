package com.seoulaim.domain.diary;

import static org.assertj.core.api.Assertions.assertThat;

import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerCategoryListResponse;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerCategoryResponse;
import com.seoulaim.domain.diary.entity.DiarySticker;
import com.seoulaim.domain.diary.repository.DiaryStickerCategoryRepository;
import com.seoulaim.domain.diary.repository.DiaryStickerRepository;
import com.seoulaim.domain.diary.service.DiaryStickerCategoryService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@SpringBootTest
@Testcontainers
class StickerCategoryIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @Autowired private DiaryStickerCategoryService categoryService;
  @Autowired private DiaryStickerCategoryRepository categoryRepository;
  @Autowired private DiaryStickerRepository stickerRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void cleanUp() {
    jdbcTemplate.update("DELETE FROM diary_sticker_category_map");
    jdbcTemplate.update("DELETE FROM diary_sticker_categories");
  }

  private void map(UUID stickerId, UUID categoryId) {
    jdbcTemplate.update(
        "INSERT INTO diary_sticker_category_map (sticker_id, category_id) VALUES (?, ?)",
        stickerId,
        categoryId);
  }

  private int mappingCount(UUID categoryId) {
    return jdbcTemplate.queryForObject(
        "SELECT count(*) FROM diary_sticker_category_map WHERE category_id = ?",
        Integer.class,
        categoryId);
  }

  @Test
  @DisplayName("마이그레이션 후 카테고리 생성·정렬·활성 스티커 수 집계·삭제 시 매핑만 제거가 동작한다")
  void categoryLifecycle_마이그레이션적용_정상동작() {
    AdminDiaryStickerCategoryResponse cute = categoryService.createCategory("Cute");
    AdminDiaryStickerCategoryResponse food = categoryService.createCategory("Food");
    assertThat(cute.displayOrder()).isZero();
    assertThat(food.displayOrder()).isEqualTo(1);

    DiarySticker active =
        stickerRepository.save(DiarySticker.create("heart", "k/heart.png", 0, true));
    DiarySticker inactive =
        stickerRepository.save(DiarySticker.create("old", "k/old.png", 1, false));
    map(active.getId(), cute.id());
    map(inactive.getId(), cute.id());
    map(active.getId(), food.id());

    categoryService.reorderCategories(List.of(food.id()));
    List<AdminDiaryStickerCategoryListResponse> list = categoryService.getCategories();
    assertThat(list)
        .extracting(AdminDiaryStickerCategoryListResponse::name)
        .containsExactly("Food", "Cute");
    // 비활성 스티커는 세지 않는다
    assertThat(list)
        .extracting(AdminDiaryStickerCategoryListResponse::stickerCount)
        .containsExactly(1L, 1L);

    categoryService.deleteCategory(cute.id());

    assertThat(categoryRepository.existsById(cute.id())).isFalse();
    assertThat(mappingCount(cute.id())).isZero();
    assertThat(mappingCount(food.id())).isEqualTo(1);
    assertThat(stickerRepository.existsById(active.getId())).isTrue();
    assertThat(stickerRepository.existsById(inactive.getId())).isTrue();
  }

  @Test
  @DisplayName("카테고리 행을 직접 지워도 FK ON DELETE CASCADE 로 매핑이 함께 지워진다")
  void deleteCategoryRow_매핑있음_cascade삭제() {
    AdminDiaryStickerCategoryResponse cute = categoryService.createCategory("Cute");
    DiarySticker sticker =
        stickerRepository.save(DiarySticker.create("star", "k/star.png", 0, true));
    map(sticker.getId(), cute.id());

    jdbcTemplate.update("DELETE FROM diary_sticker_categories WHERE id = ?", cute.id());

    assertThat(mappingCount(cute.id())).isZero();
    assertThat(stickerRepository.existsById(sticker.getId())).isTrue();
  }
}
