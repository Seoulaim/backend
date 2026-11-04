package com.seoulaim.domain.content.dto;

import com.seoulaim.domain.content.entity.Banner;
import com.seoulaim.domain.content.entity.BannerTargetType;
import java.util.Map;
import java.util.UUID;

/** 다이어리 배너 (DD01). 홈 배너 + 버튼 문구(ctaLabel). */
public record DiaryBannerResponse(
    UUID id,
    Map<String, String> title,
    Map<String, String> description,
    Map<String, String> ctaLabel,
    String imageKey,
    BannerTargetType targetType,
    String targetUrl) {

  public static DiaryBannerResponse from(Banner banner) {
    return new DiaryBannerResponse(
        banner.getId(),
        banner.getTitle(),
        banner.getDescription(),
        banner.getCtaLabel(),
        banner.getImageKey(),
        banner.getTargetType(),
        banner.getTargetUrl());
  }
}
