package com.seoulaim.domain.goods.service;

import com.seoulaim.domain.file.dto.FileResponse;
import com.seoulaim.domain.file.service.FileService;
import com.seoulaim.domain.goods.admin.dto.AdminProductDetailResponse;
import com.seoulaim.domain.goods.admin.dto.AdminProductImageFileResponse;
import com.seoulaim.domain.goods.admin.dto.AdminProductRequest;
import com.seoulaim.domain.goods.admin.dto.AdminProductSummaryResponse;
import com.seoulaim.domain.goods.dto.ProductCursor;
import com.seoulaim.domain.goods.dto.ProductDetailResponse;
import com.seoulaim.domain.goods.dto.ProductSearchCondition;
import com.seoulaim.domain.goods.dto.ProductSort;
import com.seoulaim.domain.goods.dto.ProductSummaryResponse;
import com.seoulaim.domain.goods.dto.ProductThumbnail;
import com.seoulaim.domain.goods.entity.Product;
import com.seoulaim.domain.goods.entity.ProductCategory;
import com.seoulaim.domain.goods.entity.ProductImage;
import com.seoulaim.domain.goods.repository.ProductCategoryRepository;
import com.seoulaim.domain.goods.repository.ProductImageRepository;
import com.seoulaim.domain.goods.repository.ProductRepository;
import com.seoulaim.domain.goods.repository.ProductSpecifications;
import com.seoulaim.global.common.CursorPageResponse;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

  static final int MAX_BEST_SIZE = 20;
  static final int MAX_PAGE_SIZE = 50;

  private final ProductRepository productRepository;
  private final ProductImageRepository productImageRepository;
  private final ProductCategoryRepository productCategoryRepository;
  private final FileService fileService;

  public ProductDetailResponse getById(UUID productId) {
    Product product = findProduct(productId);
    List<ProductImage> images =
        productImageRepository.findAllByProductIdOrderByDisplayOrderAscIdAsc(productId);
    return ProductDetailResponse.from(product, images);
  }

  public List<ProductSummaryResponse> getBestProducts(int size) {
    int limitedSize = Math.clamp(size, 1, MAX_BEST_SIZE);
    List<Product> products =
        productRepository.findAllByOrderBySalesCount30dDescIdDesc(PageRequest.of(0, limitedSize));
    return toSummaries(products);
  }

  public CursorPageResponse<ProductSummaryResponse> getProducts(ProductSearchCondition condition) {
    int size = Math.clamp(condition.size(), 1, MAX_PAGE_SIZE);
    ProductSort sort = condition.sort();

    Specification<Product> specification =
        Specification.allOf(
            ProductSpecifications.categoryIdEquals(condition.categoryId()),
            ProductSpecifications.artistGroupIdEquals(condition.artistGroupId()),
            ProductSpecifications.artistIdEquals(condition.artistId()),
            ProductSpecifications.after(sort, condition.cursor()));
    Sort order = Sort.by(Sort.Direction.DESC, sort.getProperty(), "id");

    // size + 1 개를 조회해 다음 페이지 존재 여부를 판단한다
    List<Product> fetched =
        productRepository.findBy(specification, query -> query.sortBy(order).limit(size + 1).all());

    boolean hasNext = fetched.size() > size;
    List<Product> products = hasNext ? fetched.subList(0, size) : fetched;
    String nextCursor = hasNext ? ProductCursor.of(sort, products.getLast()).encode() : null;

    return new CursorPageResponse<>(toSummaries(products), nextCursor);
  }

  public List<AdminProductSummaryResponse> getAdminProducts() {
    List<Product> products = productRepository.findTop200ByOrderByCreatedAtDescIdDesc();
    Map<UUID, String> thumbnails = findThumbnails(products);
    return products.stream()
        .map(product -> AdminProductSummaryResponse.from(product, thumbnails.get(product.getId())))
        .toList();
  }

  public AdminProductDetailResponse getAdminProduct(UUID productId) {
    ProductDetailResponse detail = getById(productId);
    List<String> keys = new ArrayList<>(detail.imageKeys());
    if (detail.detailImageKey() != null) {
      keys.add(detail.detailImageKey());
    }
    Map<String, UUID> fileIdsByKey =
        fileService.getByS3Keys(keys).stream()
            .collect(Collectors.toMap(FileResponse::s3Key, FileResponse::fileId));

    List<AdminProductImageFileResponse> imageFileKeys =
        detail.imageKeys().stream()
            .map(key -> new AdminProductImageFileResponse(fileIdsByKey.get(key), key))
            .toList();
    UUID detailImageFileId =
        detail.detailImageKey() == null ? null : fileIdsByKey.get(detail.detailImageKey());
    return AdminProductDetailResponse.of(detail, imageFileKeys, detailImageFileId);
  }

  @Transactional
  public UUID create(AdminProductRequest request) {
    ProductCategory category = findCategory(request.categoryId());
    LinkedImageKeys imageKeys = linkImages(request);

    Product product =
        productRepository.save(
            Product.create(
                category,
                request.artistGroupId(),
                request.artistId(),
                request.name(),
                request.summary(),
                imageKeys.detailImageKey(),
                request.price(),
                request.isDiary()));
    saveImages(product, imageKeys.imageKeys());
    return product.getId();
  }

  /** 이미지 목록은 전체 교체한다. 교체로 빠진 파일은 LINKED 로 남는다. */
  @Transactional
  public UUID update(UUID productId, AdminProductRequest request) {
    Product product = findProduct(productId);
    ProductCategory category = findCategory(request.categoryId());
    LinkedImageKeys imageKeys = linkImages(request);

    product.update(
        category,
        request.artistGroupId(),
        request.artistId(),
        request.name(),
        request.summary(),
        imageKeys.detailImageKey(),
        request.price(),
        request.isDiary());
    productImageRepository.deleteAllByProductId(productId);
    saveImages(product, imageKeys.imageKeys());
    return product.getId();
  }

  @Transactional
  public void delete(UUID productId) {
    Product product = findProduct(productId);
    // TODO(#24): order_items 테이블 반영 후 주문 이력이 있는 상품은 삭제를 막는다
    productImageRepository.deleteAllByProductId(productId);
    productRepository.delete(product);
  }

  private Product findProduct(UUID productId) {
    return productRepository
        .findById(productId)
        .orElseThrow(() -> new BusinessException(ErrorCode.GOODS_NOT_FOUND));
  }

  private ProductCategory findCategory(UUID categoryId) {
    return productCategoryRepository
        .findById(categoryId)
        .orElseThrow(() -> new BusinessException(ErrorCode.GOODS_CATEGORY_NOT_FOUND));
  }

  // 업로드된 파일을 LINKED 로 바꾸고 s3 key 를 꺼낸다. 없는 fileId 가 있으면 FILE_NOT_FOUND
  private LinkedImageKeys linkImages(AdminProductRequest request) {
    List<UUID> fileIds = new ArrayList<>(request.imageFileIds());
    if (request.detailImageFileId() != null) {
      fileIds.add(request.detailImageFileId());
    }
    fileService.markLinked(fileIds);

    List<String> imageKeys =
        request.imageFileIds().stream().map(fileId -> fileService.getById(fileId).s3Key()).toList();
    String detailImageKey =
        request.detailImageFileId() == null
            ? null
            : fileService.getById(request.detailImageFileId()).s3Key();
    return new LinkedImageKeys(imageKeys, detailImageKey);
  }

  private void saveImages(Product product, List<String> imageKeys) {
    List<ProductImage> images = new ArrayList<>();
    for (int order = 0; order < imageKeys.size(); order++) {
      images.add(ProductImage.create(product, imageKeys.get(order), order));
    }
    productImageRepository.saveAll(images);
  }

  private List<ProductSummaryResponse> toSummaries(List<Product> products) {
    Map<UUID, String> thumbnails = findThumbnails(products);
    return products.stream()
        .map(product -> ProductSummaryResponse.from(product, thumbnails.get(product.getId())))
        .toList();
  }

  private Map<UUID, String> findThumbnails(List<Product> products) {
    if (products.isEmpty()) {
      return Map.of();
    }
    List<UUID> productIds = products.stream().map(Product::getId).toList();
    // display_order 오름차순으로 정렬돼 오므로 상품별 첫 번째 값이 썸네일
    return productImageRepository.findThumbnailCandidates(productIds).stream()
        .collect(
            Collectors.toMap(
                ProductThumbnail::productId,
                ProductThumbnail::imageKey,
                (first, ignored) -> first));
  }

  private record LinkedImageKeys(List<String> imageKeys, String detailImageKey) {}
}
