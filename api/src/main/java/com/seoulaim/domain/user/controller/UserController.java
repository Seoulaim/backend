package com.seoulaim.domain.user.controller;

import com.seoulaim.domain.user.dto.NotificationSettingsResponse;
import com.seoulaim.domain.user.dto.UpdateLanguageRequest;
import com.seoulaim.domain.user.dto.UpdateNotificationSettingsRequest;
import com.seoulaim.domain.user.dto.UpdateProfileRequest;
import com.seoulaim.domain.user.dto.UserResponse;
import com.seoulaim.domain.user.service.UserService;
import com.seoulaim.global.common.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @Operation(summary = "내 정보 조회")
  @GetMapping("/me")
  public UserResponse getMe(@CurrentUserId UUID userId) {
    return userService.getUserProfile(userId);
  }

  @Operation(summary = "내 프로필 수정 (닉네임·프로필 이미지)")
  @PatchMapping("/me")
  public UserResponse updateMe(
      @CurrentUserId UUID userId, @RequestBody UpdateProfileRequest request) {
    return userService.updateProfile(userId, request);
  }

  @Operation(summary = "메일 수신 설정 조회")
  @GetMapping("/me/notifications")
  public NotificationSettingsResponse getNotifications(@CurrentUserId UUID userId) {
    return userService.getNotificationSettings(userId);
  }

  @Operation(summary = "메일 수신 설정 변경 (마케팅)")
  @PatchMapping("/me/notifications")
  public NotificationSettingsResponse updateNotifications(
      @CurrentUserId UUID userId, @RequestBody UpdateNotificationSettingsRequest request) {
    return userService.updateNotificationSettings(userId, request.marketing());
  }

  @Operation(summary = "앱 언어 변경")
  @PatchMapping("/me/language")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateLanguage(
      @CurrentUserId UUID userId, @Valid @RequestBody UpdateLanguageRequest request) {
    userService.updateLanguage(userId, request.language());
  }
}
