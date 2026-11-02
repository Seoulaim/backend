package com.seoulaim.domain.diary.admin.dto;

import com.seoulaim.domain.diary.entity.TemplateFrame;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * 템플릿 생성·수정 공용.
 *
 * <ul>
 *   <li>isActive 생략: 생성은 true, 수정은 기존 값 유지
 *   <li>overlayFileId 생략: 생성은 오버레이 없음, 수정은 기존 값 유지
 *   <li>aspect 생략: 생성은 기본값(297/210), 수정은 기존 값 유지
 *   <li>frames 생략: 생성은 빈 배열, 수정은 기존 값 유지 (보내면 전체 교체)
 * </ul>
 */
public record AdminDiaryTemplateRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description,
    @NotNull UUID imageFileId,
    @NotNull Integer displayOrder,
    Boolean isActive,
    UUID overlayFileId,
    @Positive Double aspect,
    List<@Valid @NotNull TemplateFrame> frames) {}
