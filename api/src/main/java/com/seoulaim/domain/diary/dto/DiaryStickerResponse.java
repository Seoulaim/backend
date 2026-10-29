package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.diary.entity.DiarySticker;
import java.util.UUID;

public record DiaryStickerResponse(UUID id, String name, String imageKey) {

  public static DiaryStickerResponse from(DiarySticker sticker) {
    return new DiaryStickerResponse(sticker.getId(), sticker.getName(), sticker.getImageKey());
  }
}
