package com.seoulaim.domain.content.admin.dto;

import com.seoulaim.domain.content.entity.BannerPlacement;
import com.seoulaim.domain.content.entity.BannerTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * 배너 생성·수정 공용 (어드민 promotions.js 와 같은 필드명)
 *
 * <ul>
 *   <li>placement: 생성 시 필수. 수정에선 무시 (위치는 바꿀 수 없음)
 *   <li>title·description·ctaLabel: { en, ko, zh, ja }. title 의 en 필수, ctaLabel 은 DIARY 만 앱에 내려감
 *   <li>targetType URL 이면 targetUrl 필수
 *   <li>startDate·endDate: KST, 종료일 포함. 생략하면 그쪽 제한 없음. 기본 배너는 무시하고 null
 *   <li>imageFileId 생략: 생성은 사진 없음, 수정은 기존 유지. removeImage=true 면 사진 제거
 * </ul>
 */
public record AdminBannerRequest(
    BannerPlacement placement,
    @NotBlank @Size(max = 100) String adminTitle,
    UUID imageFileId,
    Boolean removeImage,
    @NotNull Map<String, String> title,
    Map<String, String> description,
    Map<String, String> ctaLabel,
    @NotNull BannerTargetType targetType,
    @Size(max = 1000) String targetUrl,
    LocalDate startDate,
    LocalDate endDate) {}
