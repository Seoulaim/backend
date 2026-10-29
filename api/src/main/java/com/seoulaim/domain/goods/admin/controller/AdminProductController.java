package com.seoulaim.domain.goods.admin.controller;

import com.seoulaim.domain.goods.admin.dto.AdminProductDetailResponse;
import com.seoulaim.domain.goods.admin.dto.AdminProductIdResponse;
import com.seoulaim.domain.goods.admin.dto.AdminProductRequest;
import com.seoulaim.domain.goods.admin.dto.AdminProductSummaryResponse;
import com.seoulaim.domain.goods.service.ProductService;
import com.seoulaim.global.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {

  private final ProductService productService;

  @Operation(summary = "[관리자] 상품 목록 조회 (최신순, 최대 200개)")
  @GetMapping
  public List<AdminProductSummaryResponse> getProducts() {
    return productService.getAdminProducts();
  }

  @Operation(summary = "[관리자] 상품 상세 조회 (이미지별 fileId 포함)")
  @GetMapping("/{productId}")
  public AdminProductDetailResponse getProduct(@PathVariable UUID productId) {
    return productService.getAdminProduct(productId);
  }

  @Operation(summary = "[관리자] 상품 등록")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AdminProductIdResponse createProduct(@Valid @RequestBody AdminProductRequest request) {
    return new AdminProductIdResponse(productService.create(request));
  }

  @Operation(summary = "[관리자] 상품 수정 (이미지 목록 전체 교체)")
  @PutMapping("/{productId}")
  public AdminProductIdResponse updateProduct(
      @PathVariable UUID productId, @Valid @RequestBody AdminProductRequest request) {
    return new AdminProductIdResponse(productService.update(productId, request));
  }

  @Operation(summary = "[관리자] 상품 삭제")
  @DeleteMapping("/{productId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteProduct(@PathVariable UUID productId) {
    productService.delete(productId);
  }
}
