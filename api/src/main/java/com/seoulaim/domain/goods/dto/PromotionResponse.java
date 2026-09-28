package com.seoulaim.domain.goods.dto;

import com.seoulaim.domain.goods.entity.Promotion;
import java.util.UUID;

public record PromotionResponse(
    UUID id, String title, String imageKey, UUID productId, UUID artistGroupId, UUID artistId) {

  public static PromotionResponse from(Promotion promotion) {
    return new PromotionResponse(
        promotion.getId(),
        promotion.getTitle(),
        promotion.getImageKey(),
        promotion.getProduct() == null ? null : promotion.getProduct().getId(),
        promotion.getArtistGroupId(),
        promotion.getArtistId());
  }
}
