package com.seoulaim.domain.user.dto;

import com.seoulaim.domain.user.entity.User;
import java.time.Instant;

/** 메일 수신 설정. service(거래·계정 메일)는 끌 수 없어 항상 true */
public record NotificationSettingsResponse(
    String email, boolean service, boolean marketing, Instant marketingUpdatedAt) {

  public static NotificationSettingsResponse from(User user) {
    return new NotificationSettingsResponse(
        user.getEmail(), true, user.isMarketingEnabled(), user.getMarketingUpdatedAt());
  }
}
