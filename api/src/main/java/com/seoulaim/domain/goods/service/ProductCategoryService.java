package com.seoulaim.domain.goods.service;

import com.seoulaim.domain.goods.admin.dto.AdminProductCategoryRequest;
import com.seoulaim.domain.goods.dto.ProductCategoryResponse;
import com.seoulaim.domain.goods.entity.ProductCategory;
import com.seoulaim.domain.goods.repository.ProductCategoryRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductCategoryService {

  private final ProductCategoryRepository productCategoryRepository;

  public List<ProductCategoryResponse> getCategories() {
    return productCategoryRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
        .map(ProductCategoryResponse::from)
        .toList();
  }

  @Transactional
  public ProductCategoryResponse create(AdminProductCategoryRequest request) {
    ProductCategory category =
        productCategoryRepository.save(
            ProductCategory.create(request.name(), request.displayOrder()));
    return ProductCategoryResponse.from(category);
  }

  @Transactional
  public ProductCategoryResponse update(UUID categoryId, AdminProductCategoryRequest request) {
    ProductCategory category =
        productCategoryRepository
            .findById(categoryId)
            .orElseThrow(() -> new BusinessException(ErrorCode.GOODS_CATEGORY_NOT_FOUND));
    category.update(request.name(), request.displayOrder());
    return ProductCategoryResponse.from(category);
  }
}
