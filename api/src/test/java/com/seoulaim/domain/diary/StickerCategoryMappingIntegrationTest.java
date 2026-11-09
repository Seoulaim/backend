package com.seoulaim.domain.diary;

import static org.assertj.core.api.Assertions.assertThat;

import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerCategoryResponse;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerRequest;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerResponse;
import com.seoulaim.domain.diary.dto.DiaryEditorStickersResponse;
import com.seoulaim.domain.diary.entity.DiarySticker;
import com.seoulaim.domain.diary.repository.DiaryStickerRepository;
import com.seoulaim.domain.diary.service.DiaryAssetService;
import com.seoulaim.domain.diary.service.DiaryStickerCategoryService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@SpringBootTest
@Testcontainers
class StickerCategoryMappingIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @Autowired private DiaryStickerCategoryService categoryService;
  @Autowired private DiaryAssetService diaryAssetService;
  @Autowired private DiaryStickerRepository stickerRepository;
  @Autowired private TransactionTemplate transactionTemplate;

  private List<UUID> storedCategoryIds(UUID stickerId) {
    return transactionTemplate.execute(
        status ->
            List.copyOf(stickerRepository.findById(stickerId).orElseThrow().getCategoryIds()));
  }

  @Test
  @DisplayName("스티커 categoryIds 가 매핑 테이블에 저장·유지·교체되고, 카테고리 삭제 시 매핑만 사라진다")
  void stickerCategories_저장교체해제_조회반영() {
    AdminDiaryStickerCategoryResponse cute = categoryService.createCategory("Cute");
    AdminDiaryStickerCategoryResponse food = categoryService.createCategory("Food");
    DiarySticker sticker = DiarySticker.create("heart", "diary_sticker/heart.png", 0, true);
    sticker.replaceCategories(List.of(food.id(), cute.id()));
    UUID stickerId = stickerRepository.save(sticker).getId();

    assertThat(storedCategoryIds(stickerId)).containsExactlyInAnyOrder(cute.id(), food.id());

    DiaryEditorStickersResponse editor = diaryAssetService.getEditorStickers();
    assertThat(editor.categories()).extracting("id").contains(cute.id(), food.id());
    assertThat(editor.stickers())
        .filteredOn(s -> s.id().equals(stickerId))
        .singleElement()
        .satisfies(s -> assertThat(s.categoryIds()).containsExactly(cute.id(), food.id()));

    // categoryIds 생략 → 유지
    diaryAssetService.updateSticker(
        stickerId, new AdminDiaryStickerRequest("heart2", null, 0, null));
    assertThat(storedCategoryIds(stickerId)).hasSize(2);

    // 교체
    AdminDiaryStickerResponse replaced =
        diaryAssetService.updateSticker(
            stickerId, new AdminDiaryStickerRequest("heart2", null, 0, null, List.of(food.id())));
    assertThat(replaced.categoryIds()).containsExactly(food.id());
    assertThat(storedCategoryIds(stickerId)).containsExactly(food.id());

    // 카테고리 삭제 → 매핑만 사라지고 스티커는 남음
    categoryService.deleteCategory(food.id());
    assertThat(storedCategoryIds(stickerId)).isEmpty();
    assertThat(diaryAssetService.getAllStickers())
        .filteredOn(s -> s.id().equals(stickerId))
        .singleElement()
        .satisfies(s -> assertThat(s.categoryIds()).isEmpty());
  }
}
