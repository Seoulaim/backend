package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.diary.entity.DiaryStickerCategory;
import java.util.UUID;

public record DiaryStickerCategoryResponse(UUID id, String name, int displayOrder) {

  public static DiaryStickerCategoryResponse from(DiaryStickerCategory category) {
    return new DiaryStickerCategoryResponse(
        category.getId(), category.getName(), category.getDisplayOrder());
  }
}
