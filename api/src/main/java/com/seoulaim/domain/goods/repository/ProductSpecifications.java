package com.seoulaim.domain.goods.repository;

import com.seoulaim.domain.goods.dto.ProductCursor;
import com.seoulaim.domain.goods.dto.ProductSort;
import com.seoulaim.domain.goods.entity.Product;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProductSpecifications {

  private static final String ID = "id";

  public static Specification<Product> categoryIdEquals(UUID categoryId) {
    return (root, query, cb) ->
        categoryId == null ? null : cb.equal(root.get("category").get(ID), categoryId);
  }

  public static Specification<Product> artistGroupIdEquals(UUID artistGroupId) {
    return (root, query, cb) ->
        artistGroupId == null ? null : cb.equal(root.get("artistGroupId"), artistGroupId);
  }

  public static Specification<Product> artistIdEquals(UUID artistId) {
    return (root, query, cb) -> artistId == null ? null : cb.equal(root.get("artistId"), artistId);
  }

  /** (sortValue, id) 내림차순 기준으로 cursor 다음 항목만 남긴다. */
  public static Specification<Product> after(ProductSort sort, ProductCursor cursor) {
    if (cursor == null) {
      return (root, query, cb) -> null;
    }
    return switch (sort) {
      case POPULAR -> keysetAfter(sort.getProperty(), toSalesCount(cursor), cursor.id());
      case LATEST ->
          keysetAfter(
              sort.getProperty(),
              Instant.EPOCH.plus(cursor.sortValue(), ChronoUnit.MICROS),
              cursor.id());
    };
  }

  private static <T extends Comparable<? super T>> Specification<Product> keysetAfter(
      String property, T value, UUID id) {
    return (root, query, cb) ->
        cb.or(
            cb.lessThan(root.<T>get(property), value),
            cb.and(cb.equal(root.get(property), value), cb.lessThan(root.<UUID>get(ID), id)));
  }

  private static Integer toSalesCount(ProductCursor cursor) {
    if (cursor.sortValue() < Integer.MIN_VALUE || cursor.sortValue() > Integer.MAX_VALUE) {
      throw new BusinessException(ErrorCode.GOODS_INVALID_CURSOR);
    }
    return (int) cursor.sortValue();
  }
}
