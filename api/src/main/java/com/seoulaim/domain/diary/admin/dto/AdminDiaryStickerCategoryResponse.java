package com.seoulaim.domain.diary.admin.dto;

import com.seoulaim.domain.diary.entity.DiaryStickerCategory;
import java.util.UUID;

/** 카테고리 생성·수정 응답. */
public record AdminDiaryStickerCategoryResponse(UUID id, String name, int displayOrder) {

  public static AdminDiaryStickerCategoryResponse from(DiaryStickerCategory category) {
    return new AdminDiaryStickerCategoryResponse(
        category.getId(), category.getName(), category.getDisplayOrder());
  }
}
