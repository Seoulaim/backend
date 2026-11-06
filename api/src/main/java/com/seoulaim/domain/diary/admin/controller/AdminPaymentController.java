package com.seoulaim.domain.diary.admin.controller;

import com.seoulaim.domain.diary.admin.dto.AdminPaymentDetailResponse;
import com.seoulaim.domain.diary.admin.dto.AdminPaymentPageResponse;
import com.seoulaim.domain.diary.service.AdminPaymentService;
import com.seoulaim.global.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 어드민 결제 관리 (다이어리 결제). */
@Tag(name = "Admin")
@SecurityRequirement(name = SwaggerConfig.ADMIN_KEY_SCHEME)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/payments")
public class AdminPaymentController {

  private final AdminPaymentService adminPaymentService;

  @Operation(summary = "[관리자] 결제 목록 (최신순, 상태·검색어·기간 필터, 합계)")
  @GetMapping
  public AdminPaymentPageResponse getPayments(
      @Parameter(description = "pending | paid | failed | refunded") @RequestParam(required = false)
          String status,
      @Parameter(description = "주문번호·이메일·닉네임 부분 일치") @RequestParam(required = false) String q,
      @Parameter(description = "생성일(KST) 시작, YYYY-MM-DD")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @Parameter(description = "생성일(KST) 끝(포함), YYYY-MM-DD")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return adminPaymentService.getPayments(status, q, from, to, page, size);
  }

  @Operation(summary = "[관리자] 결제 상세 (토스 키, 실패 사유, 환불 요청)")
  @GetMapping("/{paymentId}")
  public AdminPaymentDetailResponse getPayment(@PathVariable UUID paymentId) {
    return adminPaymentService.getPayment(paymentId);
  }
}
