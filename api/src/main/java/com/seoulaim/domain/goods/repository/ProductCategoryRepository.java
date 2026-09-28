package com.seoulaim.domain.goods.repository;

import com.seoulaim.domain.goods.entity.ProductCategory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {

  List<ProductCategory> findAllByOrderByDisplayOrderAscIdAsc();
}
