package com.seoulaim.domain.diary.admin.dto;

import com.seoulaim.domain.diary.entity.DiarySticker;
import java.util.List;
import java.util.UUID;

/** categoryIds 는 카테고리 노출 순서. */
public record AdminDiaryStickerResponse(
    UUID id,
    String name,
    String imageKey,
    int displayOrder,
    boolean isActive,
    List<UUID> categoryIds) {

  public static AdminDiaryStickerResponse of(DiarySticker sticker, List<UUID> categoryIds) {
    return new AdminDiaryStickerResponse(
        sticker.getId(),
        sticker.getName(),
        sticker.getImageKey(),
        sticker.getDisplayOrder(),
        sticker.isActive(),
        categoryIds);
  }
}
