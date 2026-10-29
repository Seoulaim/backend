package com.seoulaim.domain.goods.dto;

import com.seoulaim.domain.goods.entity.Product;
import java.util.UUID;

/** 주문 생성 시 order 도메인이 스냅샷으로 저장하는 상품 정보. */
public record ProductOrderSnapshot(UUID id, String name, long price, boolean isDiary) {

  public static ProductOrderSnapshot from(Product product) {
    return new ProductOrderSnapshot(
        product.getId(), product.getName(), product.getPrice(), product.isDiary());
  }
}
