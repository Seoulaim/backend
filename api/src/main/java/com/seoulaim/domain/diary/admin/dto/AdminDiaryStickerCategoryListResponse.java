package com.seoulaim.domain.diary.admin.dto;

import com.seoulaim.domain.diary.entity.DiaryStickerCategory;
import java.util.UUID;

/** 카테고리 목록 항목. stickerCount 는 활성 스티커 수. */
public record AdminDiaryStickerCategoryListResponse(
    UUID id, String name, int displayOrder, long stickerCount) {

  public static AdminDiaryStickerCategoryListResponse of(
      DiaryStickerCategory category, long stickerCount) {
    return new AdminDiaryStickerCategoryListResponse(
        category.getId(), category.getName(), category.getDisplayOrder(), stickerCount);
  }
}
