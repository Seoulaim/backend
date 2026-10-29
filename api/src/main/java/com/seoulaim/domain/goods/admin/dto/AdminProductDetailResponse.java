package com.seoulaim.domain.goods.admin.dto;

import com.seoulaim.domain.goods.dto.ProductDetailResponse;
import java.util.List;
import java.util.UUID;

/** 공개 상세 + 이미지별 fileId. 수정(PUT) 시 imageFileIds / detailImageFileId 를 그대로 다시 보낼 수 있게 내려준다. */
public record AdminProductDetailResponse(
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
    String detailImageKey,
    List<AdminProductImageFileResponse> imageFileKeys,
    UUID detailImageFileId) {

  public static AdminProductDetailResponse of(
      ProductDetailResponse detail,
      List<AdminProductImageFileResponse> imageFileKeys,
      UUID detailImageFileId) {
    return new AdminProductDetailResponse(
        detail.id(),
        detail.name(),
        detail.summary(),
        detail.price(),
        detail.categoryId(),
        detail.categoryName(),
        detail.artistGroupId(),
        detail.artistId(),
        detail.isDiary(),
        detail.imageKeys(),
        detail.detailImageKey(),
        imageFileKeys,
        detailImageFileId);
  }
}
