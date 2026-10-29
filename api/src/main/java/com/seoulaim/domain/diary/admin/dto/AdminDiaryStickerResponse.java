package com.seoulaim.domain.diary.admin.dto;

import com.seoulaim.domain.diary.entity.DiarySticker;
import java.util.UUID;

public record AdminDiaryStickerResponse(
    UUID id, String name, String imageKey, int displayOrder, boolean isActive) {

  public static AdminDiaryStickerResponse from(DiarySticker sticker) {
    return new AdminDiaryStickerResponse(
        sticker.getId(),
        sticker.getName(),
        sticker.getImageKey(),
        sticker.getDisplayOrder(),
        sticker.isActive());
  }
}
