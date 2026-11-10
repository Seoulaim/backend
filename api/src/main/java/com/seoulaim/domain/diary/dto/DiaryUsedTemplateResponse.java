package com.seoulaim.domain.diary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seoulaim.domain.diary.entity.DiaryTemplate;
import com.seoulaim.domain.diary.entity.TemplateFrame;
import java.util.List;
import java.util.UUID;

/**
 * 편집 화면의 usedTemplates 원소. 공개 템플릿 응답과 같은 필드 + isActive.
 *
 * <p>어드민이 노출을 꺼도(is_active=false) 이미 그 템플릿을 쓴 다이어리는 그려야 하므로 전체 정보를 내려준다.
 */
public record DiaryUsedTemplateResponse(
    UUID id,
    String name,
    String description,
    String imageKey,
    String overlayImageKey,
    double aspect,
    List<TemplateFrame> frames,
    @JsonProperty("isActive") boolean isActive) {

  public static DiaryUsedTemplateResponse from(DiaryTemplate template) {
    return new DiaryUsedTemplateResponse(
        template.getId(),
        template.getName(),
        template.getDescription(),
        template.getImageKey(),
        template.getOverlayImageKey(),
        template.getAspect(),
        template.getFrames(),
        template.isActive());
  }
}
