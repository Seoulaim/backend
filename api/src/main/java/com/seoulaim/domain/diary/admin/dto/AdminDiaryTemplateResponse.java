package com.seoulaim.domain.diary.admin.dto;

import com.seoulaim.domain.diary.entity.DiaryTemplate;
import com.seoulaim.domain.diary.entity.TemplateFrame;
import java.util.List;
import java.util.UUID;

public record AdminDiaryTemplateResponse(
    UUID id,
    String name,
    String description,
    String imageKey,
    int displayOrder,
    boolean isActive,
    String overlayImageKey,
    double aspect,
    List<TemplateFrame> frames) {

  public static AdminDiaryTemplateResponse from(DiaryTemplate template) {
    return new AdminDiaryTemplateResponse(
        template.getId(),
        template.getName(),
        template.getDescription(),
        template.getImageKey(),
        template.getDisplayOrder(),
        template.isActive(),
        template.getOverlayImageKey(),
        template.getAspect(),
        template.getFrames());
  }
}
