package com.seoulaim.domain.goods;

import static org.assertj.core.api.Assertions.assertThat;

import com.seoulaim.domain.goods.entity.ProductCategory;
import com.seoulaim.domain.goods.repository.ProductCategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Tag("integration")
@SpringBootTest
@Testcontainers
class GoodsSchemaIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

  @Autowired private ProductCategoryRepository productCategoryRepository;

  @Test
  @DisplayName("goods 스키마가 적용된 상태에서 카테고리를 저장하면 UUID v7 id 로 조회된다")
  void save_whenSchemaMigrated_findByIdReturnsSavedCategory() {
    ProductCategory saved = productCategoryRepository.save(ProductCategory.create("Diary", 0));

    ProductCategory found = productCategoryRepository.findById(saved.getId()).orElseThrow();

    assertThat(found.getName()).isEqualTo("Diary");
    assertThat(found.getDisplayOrder()).isZero();
    assertThat(found.getId().version()).isEqualTo(7);
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
  }
}
