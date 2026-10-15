package com.seoulaim.domain.goods.dto;

import com.seoulaim.domain.goods.entity.Product;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/** 상품 목록 cursor. 외부에는 Base64(URL-safe) 로 인코딩한 {@code {sortValue}|{id}} 로 노출한다. */
public record ProductCursor(long sortValue, UUID id) {

  private static final String DELIMITER = "|";

  public static ProductCursor of(ProductSort sort, Product product) {
    return new ProductCursor(sort.sortValueOf(product), product.getId());
  }

  /** null 또는 빈 문자열이면 첫 페이지로 보고 null 을 반환한다. */
  public static ProductCursor decode(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }
    try {
      String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
      int delimiterIndex = decoded.indexOf(DELIMITER);
      if (delimiterIndex < 0) {
        throw new BusinessException(ErrorCode.GOODS_INVALID_CURSOR);
      }
      long sortValue = Long.parseLong(decoded.substring(0, delimiterIndex));
      UUID id = UUID.fromString(decoded.substring(delimiterIndex + 1));
      return new ProductCursor(sortValue, id);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(ErrorCode.GOODS_INVALID_CURSOR);
    }
  }

  public String encode() {
    String raw = sortValue + DELIMITER + id;
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }
}
