package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.diary.entity.DiarySticker;
import java.util.List;
import java.util.UUID;

/** categoryIds 는 카테고리 노출 순서. */
public record DiaryStickerResponse(
    UUID id, String name, String imageKey, int displayOrder, List<UUID> categoryIds) {

  public static DiaryStickerResponse of(DiarySticker sticker, List<UUID> categoryIds) {
    return new DiaryStickerResponse(
        sticker.getId(),
        sticker.getName(),
        sticker.getImageKey(),
        sticker.getDisplayOrder(),
        categoryIds);
  }
}
