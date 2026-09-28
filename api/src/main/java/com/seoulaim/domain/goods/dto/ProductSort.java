package com.seoulaim.domain.goods.dto;

import com.seoulaim.domain.goods.entity.Product;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.ToLongFunction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductSort {
  POPULAR("salesCount30d", Product::getSalesCount30d),
  // DB(timestamptz)가 마이크로초 정밀도라 밀리초로 자르면 같은 ms 안의 항목이 누락될 수 있다
  LATEST("createdAt", product -> ChronoUnit.MICROS.between(Instant.EPOCH, product.getCreatedAt()));

  private final String property;
  private final ToLongFunction<Product> sortValueExtractor;

  public long sortValueOf(Product product) {
    return sortValueExtractor.applyAsLong(product);
  }
}
