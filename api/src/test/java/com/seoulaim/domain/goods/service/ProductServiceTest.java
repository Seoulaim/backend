package com.seoulaim.domain.goods.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.file.dto.FileResponse;
import com.seoulaim.domain.file.entity.FileStatus;
import com.seoulaim.domain.file.service.FileService;
import com.seoulaim.domain.goods.admin.dto.AdminProductRequest;
import com.seoulaim.domain.goods.entity.Product;
import com.seoulaim.domain.goods.entity.ProductCategory;
import com.seoulaim.domain.goods.entity.ProductImage;
import com.seoulaim.domain.goods.repository.ProductCategoryRepository;
import com.seoulaim.domain.goods.repository.ProductImageRepository;
import com.seoulaim.domain.goods.repository.ProductRepository;
import com.seoulaim.domain.order.service.OrderItemService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock private ProductRepository productRepository;
  @Mock private ProductImageRepository productImageRepository;
  @Mock private ProductCategoryRepository productCategoryRepository;
  @Mock private FileService fileService;
  @Mock private OrderItemService orderItemService;
  @InjectMocks private ProductService productService;

  @Test
  @DisplayName("존재하지 않는 상품 ID 로 상세 조회하면 GOODS_NOT_FOUND 예외가 발생한다")
  void getById_존재하지않는ID_예외발생() {
    UUID productId = UUID.randomUUID();
    given(productRepository.findById(productId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> productService.getById(productId))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.GOODS_NOT_FOUND);
    verify(productImageRepository, never()).findAllByProductIdOrderByDisplayOrderAscIdAsc(any());
  }

  @Test
  @DisplayName("BEST 상품 size 가 최대값을 넘으면 20 개로 제한해 조회한다")
  void getBestProducts_size초과_최대20으로제한() {
    given(productRepository.findAllByOrderBySalesCount30dDescIdDesc(any())).willReturn(List.of());

    productService.getBestProducts(100);

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(productRepository).findAllByOrderBySalesCount30dDescIdDesc(pageableCaptor.capture());
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
  }

  private static final UUID IMAGE_FILE_1 = UUID.randomUUID();
  private static final UUID IMAGE_FILE_2 = UUID.randomUUID();
  private static final UUID DETAIL_FILE = UUID.randomUUID();

  private AdminProductRequest request(UUID categoryId) {
    return new AdminProductRequest(
        categoryId,
        UUID.randomUUID(),
        null,
        "Seoul Diary",
        "summary",
        25000L,
        true,
        List.of(IMAGE_FILE_1, IMAGE_FILE_2),
        DETAIL_FILE);
  }

  private void givenFiles() {
    given(fileService.getById(IMAGE_FILE_1)).willReturn(file(IMAGE_FILE_1, "product/1.jpg"));
    given(fileService.getById(IMAGE_FILE_2)).willReturn(file(IMAGE_FILE_2, "product/2.jpg"));
    given(fileService.getById(DETAIL_FILE)).willReturn(file(DETAIL_FILE, "product/detail.jpg"));
  }

  private FileResponse file(UUID fileId, String s3Key) {
    return new FileResponse(fileId, s3Key, FileStatus.LINKED, "image/jpeg");
  }

  @SuppressWarnings("unchecked")
  private List<ProductImage> captureSavedImages() {
    ArgumentCaptor<List<ProductImage>> captor = ArgumentCaptor.forClass(List.class);
    verify(productImageRepository).saveAll(captor.capture());
    return captor.getValue();
  }

  @Test
  @DisplayName("상품을 등록하면 파일을 LINKED 로 바꾸고 s3 key 를 배열 순서대로 이미지로 저장한다")
  void create_정상요청_이미지순서대로저장() {
    ProductCategory category = ProductCategory.create("Diary", 0);
    given(productCategoryRepository.findById(category.getId())).willReturn(Optional.of(category));
    given(productRepository.save(any(Product.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    givenFiles();

    UUID productId = productService.create(request(category.getId()));

    verify(fileService).markLinked(List.of(IMAGE_FILE_1, IMAGE_FILE_2, DETAIL_FILE));
    ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
    verify(productRepository).save(productCaptor.capture());
    Product saved = productCaptor.getValue();
    assertThat(saved.getId()).isEqualTo(productId);
    assertThat(saved.getCategory()).isEqualTo(category);
    assertThat(saved.getDetailImageKey()).isEqualTo("product/detail.jpg");
    assertThat(captureSavedImages())
        .extracting(ProductImage::getImageKey, ProductImage::getDisplayOrder)
        .containsExactly(tuple("product/1.jpg", 0), tuple("product/2.jpg", 1));
  }

  @Test
  @DisplayName("존재하지 않는 카테고리로 상품을 등록하면 GOODS_CATEGORY_NOT_FOUND 예외가 발생한다")
  void create_없는카테고리_예외발생() {
    UUID categoryId = UUID.randomUUID();
    given(productCategoryRepository.findById(categoryId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> productService.create(request(categoryId)))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.GOODS_CATEGORY_NOT_FOUND);
    verify(fileService, never()).markLinked(anyList());
    verify(productRepository, never()).save(any());
  }

  @Test
  @DisplayName("존재하지 않는 fileId 가 있으면 FILE_NOT_FOUND 예외가 발생하고 상품을 저장하지 않는다")
  void create_없는파일_예외발생() {
    ProductCategory category = ProductCategory.create("Diary", 0);
    given(productCategoryRepository.findById(category.getId())).willReturn(Optional.of(category));
    willThrow(new BusinessException(ErrorCode.FILE_NOT_FOUND))
        .given(fileService)
        .markLinked(anyList());

    assertThatThrownBy(() -> productService.create(request(category.getId())))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.FILE_NOT_FOUND);
    verify(productRepository, never()).save(any());
  }

  @Test
  @DisplayName("상품을 수정하면 필드를 바꾸고 기존 이미지를 지운 뒤 새 목록으로 다시 저장한다")
  void update_정상요청_이미지전체교체() {
    ProductCategory oldCategory = ProductCategory.create("Doll", 1);
    ProductCategory newCategory = ProductCategory.create("Diary", 0);
    Product product =
        Product.create(oldCategory, UUID.randomUUID(), null, "Old", null, null, 1000L, false);
    given(productRepository.findById(product.getId())).willReturn(Optional.of(product));
    given(productCategoryRepository.findById(newCategory.getId()))
        .willReturn(Optional.of(newCategory));
    givenFiles();

    productService.update(product.getId(), request(newCategory.getId()));

    assertThat(product.getName()).isEqualTo("Seoul Diary");
    assertThat(product.getCategory()).isEqualTo(newCategory);
    assertThat(product.getPrice()).isEqualTo(25000L);
    assertThat(product.isDiary()).isTrue();
    assertThat(product.getDetailImageKey()).isEqualTo("product/detail.jpg");
    InOrder order = inOrder(productImageRepository);
    order.verify(productImageRepository).deleteAllByProductId(product.getId());
    order.verify(productImageRepository).saveAll(anyList());
    assertThat(captureSavedImages())
        .extracting(ProductImage::getImageKey)
        .containsExactly("product/1.jpg", "product/2.jpg");
  }

  @Test
  @DisplayName("존재하지 않는 상품을 수정하면 GOODS_NOT_FOUND 예외가 발생한다")
  void update_없는상품_예외발생() {
    UUID productId = UUID.randomUUID();
    given(productRepository.findById(productId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> productService.update(productId, request(UUID.randomUUID())))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.GOODS_NOT_FOUND);
    verify(productImageRepository, never()).deleteAllByProductId(any());
  }

  @Test
  @DisplayName("주문 이력이 있는 상품을 삭제하면 GOODS_IN_USE 예외가 발생하고 삭제하지 않는다")
  void delete_주문이력있음_예외발생() {
    Product product =
        Product.create(
            ProductCategory.create("Doll", 1),
            UUID.randomUUID(),
            null,
            "Doll",
            null,
            null,
            1000L,
            false);
    given(productRepository.findById(product.getId())).willReturn(Optional.of(product));
    given(orderItemService.existsByProductId(product.getId())).willReturn(true);

    assertThatThrownBy(() -> productService.delete(product.getId()))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.GOODS_IN_USE);
    verify(productImageRepository, never()).deleteAllByProductId(any());
    verify(productRepository, never()).delete(any(Product.class));
  }
}
