package com.seoulaim.domain.user.dto;

import com.seoulaim.domain.user.entity.User;
import java.util.UUID;

public record UserResponse(
    UUID id, String email, String nickname, String profileImageKey, String locale) {

  public static UserResponse from(User user) {
    return new UserResponse(
        user.getId(),
        user.getEmail(),
        user.getNickname(),
        user.getProfileImageKey(),
        user.getLocale());
  }
}
