package com.seoulaim.domain.goods.controller;

import com.seoulaim.domain.goods.dto.ProductCategoryResponse;
import com.seoulaim.domain.goods.service.ProductCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Goods")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/product-categories")
public class ProductCategoryController {

  private final ProductCategoryService productCategoryService;

  @Operation(summary = "상품 카테고리 목록 조회")
  @GetMapping
  public List<ProductCategoryResponse> getCategories() {
    return productCategoryService.getCategories();
  }
}
