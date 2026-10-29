package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.diary.entity.DiaryTemplate;
import java.util.UUID;

public record DiaryTemplateResponse(UUID id, String name, String description, String imageKey) {

  public static DiaryTemplateResponse from(DiaryTemplate template) {
    return new DiaryTemplateResponse(
        template.getId(), template.getName(), template.getDescription(), template.getImageKey());
  }
}
