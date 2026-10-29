package com.seoulaim.domain.goods.admin.dto;

import com.seoulaim.domain.goods.entity.Product;
import java.time.Instant;
import java.util.UUID;

public record AdminProductSummaryResponse(
    UUID id,
    String name,
    long price,
    String categoryName,
    UUID artistGroupId,
    UUID artistId,
    boolean isDiary,
    String thumbnailImageKey,
    Instant createdAt) {

  public static AdminProductSummaryResponse from(Product product, String thumbnailImageKey) {
    return new AdminProductSummaryResponse(
        product.getId(),
        product.getName(),
        product.getPrice(),
        product.getCategory().getName(),
        product.getArtistGroupId(),
        product.getArtistId(),
        product.isDiary(),
        thumbnailImageKey,
        product.getCreatedAt());
  }
}
