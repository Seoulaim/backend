package com.seoulaim.domain.goods.dto;

import com.seoulaim.domain.goods.entity.ProductCategory;
import java.util.UUID;

public record ProductCategoryResponse(UUID id, String name, int displayOrder) {

  public static ProductCategoryResponse from(ProductCategory category) {
    return new ProductCategoryResponse(
        category.getId(), category.getName(), category.getDisplayOrder());
  }
}
