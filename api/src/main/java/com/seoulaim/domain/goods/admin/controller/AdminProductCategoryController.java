package com.seoulaim.domain.goods.admin.controller;

import com.seoulaim.domain.goods.admin.dto.AdminProductCategoryRequest;
import com.seoulaim.domain.goods.dto.ProductCategoryResponse;
import com.seoulaim.domain.goods.service.ProductCategoryService;
import com.seoulaim.global.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin")
@SecurityRequirement(name = SwaggerConfig.ADMIN_KEY_SCHEME)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/product-categories")
public class AdminProductCategoryController {

  private final ProductCategoryService productCategoryService;

  @Operation(summary = "[관리자] 상품 카테고리 등록")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProductCategoryResponse createCategory(
      @Valid @RequestBody AdminProductCategoryRequest request) {
    return productCategoryService.create(request);
  }

  @Operation(summary = "[관리자] 상품 카테고리 수정")
  @PutMapping("/{categoryId}")
  public ProductCategoryResponse updateCategory(
      @PathVariable UUID categoryId, @Valid @RequestBody AdminProductCategoryRequest request) {
    return productCategoryService.update(categoryId, request);
  }
}
