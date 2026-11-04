package com.seoulaim.domain.content.admin.dto;

import com.seoulaim.domain.content.entity.Banner;
import com.seoulaim.domain.content.entity.BannerPlacement;
import com.seoulaim.domain.content.entity.BannerTargetType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record AdminBannerResponse(
    UUID id,
    BannerPlacement placement,
    String adminTitle,
    String imageKey,
    Map<String, String> title,
    Map<String, String> description,
    Map<String, String> ctaLabel,
    BannerTargetType targetType,
    String targetUrl,
    LocalDate startDate,
    LocalDate endDate,
    boolean enabled,
    boolean isDefault,
    int displayOrder,
    Instant createdAt) {

  public static AdminBannerResponse from(Banner banner) {
    return new AdminBannerResponse(
        banner.getId(),
        banner.getPlacement(),
        banner.getAdminTitle(),
        banner.getImageKey(),
        banner.getTitle(),
        banner.getDescription(),
        banner.getCtaLabel(),
        banner.getTargetType(),
        banner.getTargetUrl(),
        banner.getStartDate(),
        banner.getEndDate(),
        banner.isEnabled(),
        banner.isDefault(),
        banner.getDisplayOrder(),
        banner.getCreatedAt());
  }
}
