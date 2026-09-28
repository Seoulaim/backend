package com.seoulaim.domain.goods.service;

import com.seoulaim.domain.goods.dto.ProductCategoryResponse;
import com.seoulaim.domain.goods.repository.ProductCategoryRepository;
import java.util.List;
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
}
