package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.diary.entity.DiaryTemplate;
import com.seoulaim.domain.diary.entity.TemplateFrame;
import java.util.List;
import java.util.UUID;

public record DiaryTemplateResponse(
    UUID id,
    String name,
    String description,
    String imageKey,
    String overlayImageKey,
    double aspect,
    List<TemplateFrame> frames) {

  public static DiaryTemplateResponse from(DiaryTemplate template) {
    return new DiaryTemplateResponse(
        template.getId(),
        template.getName(),
        template.getDescription(),
        template.getImageKey(),
        template.getOverlayImageKey(),
        template.getAspect(),
        template.getFrames());
  }
}
