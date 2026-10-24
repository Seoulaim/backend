package com.seoulaim.domain.goods.controller;

import com.seoulaim.domain.goods.dto.ProductCursor;
import com.seoulaim.domain.goods.dto.ProductDetailResponse;
import com.seoulaim.domain.goods.dto.ProductSearchCondition;
import com.seoulaim.domain.goods.dto.ProductSort;
import com.seoulaim.domain.goods.dto.ProductSummaryResponse;
import com.seoulaim.domain.goods.service.ProductService;
import com.seoulaim.global.common.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Goods")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

  private final ProductService productService;

  @Operation(summary = "상품 목록 조회 (카테고리/그룹/멤버 필터, cursor 페이징)")
  @GetMapping
  public CursorPageResponse<ProductSummaryResponse> getProducts(
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) UUID artistGroupId,
      @RequestParam(required = false) UUID artistId,
      @RequestParam(defaultValue = "POPULAR") ProductSort sort,
      @RequestParam(required = false) String cursor,
      @RequestParam(defaultValue = "20") int size) {
    ProductSearchCondition condition =
        new ProductSearchCondition(
            categoryId, artistGroupId, artistId, sort, ProductCursor.decode(cursor), size);
    return productService.getProducts(condition);
  }

  @Operation(summary = "BEST 상품 조회 (최근 30일 판매량 상위)")
  @GetMapping("/best")
  public List<ProductSummaryResponse> getBestProducts(@RequestParam(defaultValue = "4") int size) {
    return productService.getBestProducts(size);
  }

  @Operation(summary = "상품 상세 조회")
  @GetMapping("/{productId}")
  public ProductDetailResponse getProduct(@PathVariable UUID productId) {
    return productService.getById(productId);
  }
}
