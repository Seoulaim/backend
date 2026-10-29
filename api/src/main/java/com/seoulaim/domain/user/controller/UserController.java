package com.seoulaim.domain.user.controller;

import com.seoulaim.domain.user.dto.UserResponse;
import com.seoulaim.domain.user.service.UserService;
import com.seoulaim.global.common.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
