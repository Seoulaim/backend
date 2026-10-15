package com.seoulaim.domain.goods.dto;

import com.seoulaim.domain.goods.entity.Product;
import com.seoulaim.domain.goods.entity.ProductImage;
import java.util.List;
import java.util.UUID;

public record ProductDetailResponse(
    UUID id,
    String name,
    String summary,
    long price,
    UUID categoryId,
    String categoryName,
    UUID artistGroupId,
    UUID artistId,
    boolean isDiary,
    List<String> imageKeys,
    String detailImageKey) {

  public static ProductDetailResponse from(Product product, List<ProductImage> images) {
    return new ProductDetailResponse(
        product.getId(),
        product.getName(),
        product.getSummary(),
        product.getPrice(),
        product.getCategory().getId(),
        product.getCategory().getName(),
        product.getArtistGroupId(),
        product.getArtistId(),
        product.isDiary(),
        images.stream().map(ProductImage::getImageKey).toList(),
        product.getDetailImageKey());
  }
}
