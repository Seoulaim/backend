package com.seoulaim.domain.diary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiarySticker;
import com.seoulaim.domain.diary.entity.DiaryTemplate;
import com.seoulaim.domain.diary.repository.DiaryRepository;
import com.seoulaim.domain.diary.repository.DiaryStickerRepository;
import com.seoulaim.domain.diary.repository.DiaryTemplateRepository;
import com.seoulaim.domain.file.entity.File;
import com.seoulaim.domain.file.entity.FileDomain;
import com.seoulaim.domain.file.repository.FileRepository;
import com.seoulaim.domain.goods.entity.Product;
import com.seoulaim.domain.goods.entity.ProductCategory;
import com.seoulaim.domain.goods.repository.ProductCategoryRepository;
import com.seoulaim.domain.goods.repository.ProductRepository;
import com.seoulaim.domain.order.entity.ContactType;
import com.seoulaim.domain.order.entity.Order;
import com.seoulaim.domain.order.entity.OrderItem;
import com.seoulaim.domain.order.repository.OrderItemRepository;
import com.seoulaim.domain.order.repository.OrderRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@SpringBootTest
@Testcontainers
class DiarySchemaIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @Autowired private ProductCategoryRepository productCategoryRepository;
  @Autowired private ProductRepository productRepository;
  @Autowired private OrderRepository orderRepository;
  @Autowired private OrderItemRepository orderItemRepository;
  @Autowired private FileRepository fileRepository;
  @Autowired private DiaryRepository diaryRepository;
  @Autowired private DiaryTemplateRepository diaryTemplateRepository;
  @Autowired private DiaryStickerRepository diaryStickerRepository;

  @Test
  @DisplayName("diary 스키마가 적용된 상태에서 jsonb 편집 데이터를 저장하고 그대로 조회된다")
  void save_whenSchemaMigrated_contentRoundTrips() {
    UUID userId = UUID.randomUUID();
    OrderItem item = saveDiaryOrderItem(userId);
    File pdf =
        fileRepository.save(
            File.createPending(
                userId, FileDomain.DIARY, "diary/2026/10/a.pdf", "application/pdf", 2048L));
    Map<String, Object> content =
        Map.of(
            "version",
            1,
            "pages",
            List.of(
                Map.of(
                    "templateId",
                    "t1",
                    "elements",
                    List.of(
                        Map.of("type", "sticker", "x", 12.5, "rotation", -30, "text", "사랑해 ❤")))));
    Diary diary =
        diaryRepository.save(
            Diary.submit(
                userId,
                item.getId(),
                12,
                content,
                pdf.getId(),
                pdf.getS3Key(),
                Instant.parse("2026-10-29T04:00:00Z")));

    Diary found = diaryRepository.findByIdAndUserId(diary.getId(), userId).orElseThrow();

    assertThat(found.getContent()).isEqualTo(content);
    assertThat(found.getPageCount()).isEqualTo(12);
    assertThat(found.getId().version()).isEqualTo(7);
    assertThat(diaryRepository.existsByOrderItemId(item.getId())).isTrue();
    assertThat(diaryRepository.findByIdAndUserId(diary.getId(), UUID.randomUUID())).isEmpty();
    assertThat(diaryRepository.findAllByUserIdOrderBySubmittedAtDescIdDesc(userId)).hasSize(1);
    assertThatThrownBy(
            () ->
                diaryRepository.saveAndFlush(
                    Diary.submit(
                        userId,
                        item.getId(),
                        1,
                        Map.of(),
                        pdf.getId(),
                        pdf.getS3Key(),
                        Instant.parse("2026-10-29T05:00:00Z"))))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("활성 템플릿·스티커만 노출 순서대로 조회된다")
  void findActive_whenInactiveExists_returnsOnlyActiveInOrder() {
    diaryTemplateRepository.save(DiaryTemplate.create("B", null, "diary/b.png", 2, true));
    diaryTemplateRepository.save(DiaryTemplate.create("A", "기본", "diary/a.png", 1, true));
    diaryTemplateRepository.save(DiaryTemplate.create("Off", null, "diary/off.png", 0, false));
    diaryStickerRepository.save(DiarySticker.create(null, "diary/s1.png", 0, true));
    diaryStickerRepository.save(DiarySticker.create("off", "diary/s2.png", 1, false));

    assertThat(diaryTemplateRepository.findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc())
        .extracting(DiaryTemplate::getName)
        .containsExactly("A", "B");
    assertThat(diaryTemplateRepository.findAllByOrderByDisplayOrderAscIdAsc()).hasSize(3);
    assertThat(diaryStickerRepository.findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc())
        .extracting(DiarySticker::getImageKey)
        .containsExactly("diary/s1.png");
  }

  private OrderItem saveDiaryOrderItem(UUID userId) {
    ProductCategory category = productCategoryRepository.save(ProductCategory.create("Diary", 0));
    Product product =
        productRepository.save(
            Product.create(category, UUID.randomUUID(), null, "Diary", null, null, 25000L, true));
    Order order =
        orderRepository.save(
            Order.createPending(
                "ORD-20261029-DI4RY1",
                userId,
                25000L,
                3000L,
                "Kim",
                "서울특별시 중구 세종대로 110",
                null,
                null,
                ContactType.NONE,
                null,
                Instant.parse("2026-10-29T03:00:00Z")));
    return orderItemRepository.save(
        OrderItem.create(order.getId(), product.getId(), "Diary", 25000L, 1));
  }
}
