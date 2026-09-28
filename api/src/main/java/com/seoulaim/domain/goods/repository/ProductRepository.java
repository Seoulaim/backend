package com.seoulaim.domain.goods.repository;

import com.seoulaim.domain.goods.entity.Product;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {}
