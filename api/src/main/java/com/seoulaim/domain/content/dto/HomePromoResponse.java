package com.seoulaim.domain.content.dto;

import com.seoulaim.domain.content.entity.Banner;
import com.seoulaim.domain.content.entity.BannerTargetType;
import java.util.Map;
import java.util.UUID;

/** 홈 메인 배너 (HH01). imageKey 는 기본 배너처럼 null 일 수 있다. */
public record HomePromoResponse(
    UUID id,
    Map<String, String> title,
    Map<String, String> description,
    String imageKey,
    BannerTargetType targetType,
    String targetUrl) {

  public static HomePromoResponse from(Banner banner) {
    return new HomePromoResponse(
        banner.getId(),
        banner.getTitle(),
        banner.getDescription(),
        banner.getImageKey(),
        banner.getTargetType(),
        banner.getTargetUrl());
  }
}
