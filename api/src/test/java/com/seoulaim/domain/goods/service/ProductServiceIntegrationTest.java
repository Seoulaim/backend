package com.seoulaim.domain.goods.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.seoulaim.domain.goods.dto.ProductCursor;
import com.seoulaim.domain.goods.dto.ProductSearchCondition;
import com.seoulaim.domain.goods.dto.ProductSort;
import com.seoulaim.domain.goods.dto.ProductSummaryResponse;
import com.seoulaim.domain.goods.entity.Product;
import com.seoulaim.domain.goods.entity.ProductCategory;
import com.seoulaim.domain.goods.entity.ProductImage;
import com.seoulaim.domain.goods.repository.ProductCategoryRepository;
import com.seoulaim.domain.goods.repository.ProductImageRepository;
import com.seoulaim.domain.goods.repository.ProductRepository;
import com.seoulaim.global.common.CursorPageResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@SpringBootTest
@Testcontainers
class ProductServiceIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @Autowired private ProductService productService;
  @Autowired private ProductRepository productRepository;
  @Autowired private ProductImageRepository productImageRepository;
  @Autowired private ProductCategoryRepository productCategoryRepository;

  @AfterEach
  void tearDown() {
    productImageRepository.deleteAllInBatch();
    productRepository.deleteAllInBatch();
    productCategoryRepository.deleteAllInBatch();
  }

  @Test
  @DisplayName("LATEST 정렬로 cursor 를 따라가면 모든 상품을 중복·누락 없이 최신순으로 조회한다")
  void getProducts_LATEST커서순회_전체를순서대로조회() {
    ProductCategory category = productCategoryRepository.save(ProductCategory.create("Diary", 0));
    UUID groupId = UUID.randomUUID();
    List<UUID> savedIds = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      savedIds.add(
          productRepository
              .save(Product.create(category, groupId, null, "p" + i, null, null, 1000L, false))
              .getId());
    }

    List<UUID> collected = new ArrayList<>();
    ProductCursor cursor = null;
    do {
      CursorPageResponse<ProductSummaryResponse> page =
          productService.getProducts(
              new ProductSearchCondition(null, groupId, null, ProductSort.LATEST, cursor, 2));
      page.items().forEach(item -> collected.add(item.id()));
      cursor = ProductCursor.decode(page.nextCursor());
    } while (cursor != null);

    assertThat(collected).containsExactlyElementsOf(savedIds.reversed());
  }

  @Test
  @DisplayName("POPULAR 정렬 목록은 display_order 가 가장 작은 이미지를 썸네일로 내려준다")
  void getProducts_POPULAR_최소displayOrder이미지가썸네일() {
    ProductCategory category = productCategoryRepository.save(ProductCategory.create("Doll", 0));
    Product product =
        productRepository.save(
            Product.create(category, UUID.randomUUID(), null, "doll", null, null, 1000L, false));
    productImageRepository.save(ProductImage.create(product, "goods/second.jpg", 1));
    productImageRepository.save(ProductImage.create(product, "goods/first.jpg", 0));

    CursorPageResponse<ProductSummaryResponse> page =
        productService.getProducts(
            new ProductSearchCondition(
                category.getId(), null, null, ProductSort.POPULAR, null, 20));

    assertThat(page.items()).singleElement();
    assertThat(page.items().getFirst().thumbnailImageKey()).isEqualTo("goods/first.jpg");
    assertThat(page.nextCursor()).isNull();
  }
}
