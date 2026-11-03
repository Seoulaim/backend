package com.seoulaim.domain.diary.entity;

import jakarta.validation.constraints.NotBlank;

/**
 * 템플릿 안의 사진 프레임 1개. diary_templates.frames(jsonb) 배열 원소로 저장된다.
 *
 * <p>좌표·크기 단위와 기준(비율/픽셀)은 웹앱 편집기가 정하며 서버는 그대로 보관·반환한다. rotation 은 도(degree).
 */
public record TemplateFrame(
    @NotBlank String id, double x, double y, double width, double height, double rotation) {}
