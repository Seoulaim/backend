package com.seoulaim.domain.goods.service;

import com.seoulaim.domain.goods.dto.ProductCursor;
import com.seoulaim.domain.goods.dto.ProductDetailResponse;
import com.seoulaim.domain.goods.dto.ProductSearchCondition;
import com.seoulaim.domain.goods.dto.ProductSort;
import com.seoulaim.domain.goods.dto.ProductSummaryResponse;
import com.seoulaim.domain.goods.dto.ProductThumbnail;
import com.seoulaim.domain.goods.entity.Product;
import com.seoulaim.domain.goods.entity.ProductImage;
import com.seoulaim.domain.goods.repository.ProductImageRepository;
import com.seoulaim.domain.goods.repository.ProductRepository;
import com.seoulaim.domain.goods.repository.ProductSpecifications;
import com.seoulaim.global.common.CursorPageResponse;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
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

  public ProductDetailResponse getById(UUID productId) {
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.GOODS_NOT_FOUND));
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

  private List<ProductSummaryResponse> toSummaries(List<Product> products) {
    if (products.isEmpty()) {
      return List.of();
    }
    List<UUID> productIds = products.stream().map(Product::getId).toList();
    // display_order 오름차순으로 정렬돼 오므로 상품별 첫 번째 값이 썸네일
    Map<UUID, String> thumbnails =
        productImageRepository.findThumbnailCandidates(productIds).stream()
            .collect(
                Collectors.toMap(
                    ProductThumbnail::productId,
                    ProductThumbnail::imageKey,
                    (first, ignored) -> first));

    return products.stream()
        .map(product -> ProductSummaryResponse.from(product, thumbnails.get(product.getId())))
        .toList();
  }
}
