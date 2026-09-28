package com.seoulaim.domain.goods.repository;

import com.seoulaim.domain.goods.entity.Promotion;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {}
