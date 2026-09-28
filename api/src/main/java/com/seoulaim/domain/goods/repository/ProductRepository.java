package com.seoulaim.domain.goods.repository;

import com.seoulaim.domain.goods.entity.Product;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository
    extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

  List<Product> findAllByOrderBySalesCount30dDescIdDesc(Pageable pageable);
}
