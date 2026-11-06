package com.seoulaim.domain.user.admin.controller;

import com.seoulaim.domain.user.admin.dto.AdminUserDetailResponse;
import com.seoulaim.domain.user.admin.dto.AdminUserListResponse;
import com.seoulaim.domain.user.admin.dto.AdminUserWithdrawRequest;
import com.seoulaim.domain.user.service.AdminUserService;
import com.seoulaim.global.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 어드민 회원 관리. */
@Tag(name = "Admin")
@SecurityRequirement(name = SwaggerConfig.ADMIN_KEY_SCHEME)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

  private final AdminUserService adminUserService;

  @Operation(summary = "[관리자] 회원 목록 (가입 최신순, 이메일 마스킹)")
  @GetMapping
  public AdminUserListResponse getUsers(
      @Parameter(description = "이메일·닉네임 부분 일치") @RequestParam(required = false) String q,
      @Parameter(description = "active | suspended | withdrawn") @RequestParam(required = false)
          String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return adminUserService.getUsers(q, status, page, size);
  }

  @Operation(summary = "[관리자] 회원 상세 (최애·다이어리·결제·문의)")
  @GetMapping("/{userId}")
  public AdminUserDetailResponse getUser(@PathVariable UUID userId) {
    return adminUserService.getUser(userId);
  }

  @Operation(summary = "[관리자] 회원 강퇴 (탈퇴 처리 + 사유 기록, 이미 탈퇴면 409)")
  @PostMapping("/{userId}/withdraw")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void withdraw(@PathVariable UUID userId, @RequestBody AdminUserWithdrawRequest request) {
    adminUserService.withdraw(userId, request.reason());
  }
}
