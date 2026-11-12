package com.seoulaim.domain.dashboard.admin.controller;

import com.seoulaim.domain.dashboard.admin.dto.AdminDashboardResponse;
import com.seoulaim.domain.dashboard.service.AdminDashboardService;
import com.seoulaim.global.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 어드민 대시보드. */
@Tag(name = "Admin")
@SecurityRequirement(name = SwaggerConfig.ADMIN_KEY_SCHEME)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {

  private final AdminDashboardService adminDashboardService;

  @Operation(summary = "[관리자] 대시보드 (가입·결제·다이어리·처리 대기·설문, KST 기준)")
  @GetMapping
  public AdminDashboardResponse getDashboard(
      @Parameter(description = "7d | 30d | 90d (기본 7d)") @RequestParam(required = false)
          String range) {
    return adminDashboardService.getDashboard(range);
  }
}
