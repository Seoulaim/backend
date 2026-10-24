package com.seoulaim.domain.goods.dto;

import com.seoulaim.domain.goods.entity.Product;
import java.util.UUID;

public record ProductSummaryResponse(
    UUID id,
    String name,
    long price,
    String thumbnailImageKey,
    UUID artistGroupId,
    UUID artistId,
    UUID categoryId) {

  public static ProductSummaryResponse from(Product product, String thumbnailImageKey) {
    return new ProductSummaryResponse(
        product.getId(),
        product.getName(),
        product.getPrice(),
        thumbnailImageKey,
        product.getArtistGroupId(),
        product.getArtistId(),
        product.getCategory().getId());
  }
}
