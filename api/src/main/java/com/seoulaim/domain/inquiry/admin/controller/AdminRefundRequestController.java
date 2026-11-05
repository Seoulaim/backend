package com.seoulaim.domain.inquiry.admin.controller;

import com.seoulaim.domain.inquiry.admin.dto.AdminRefundApproveRequest;
import com.seoulaim.domain.inquiry.admin.dto.AdminRefundRejectRequest;
import com.seoulaim.domain.inquiry.admin.dto.AdminRefundRequestResponse;
import com.seoulaim.domain.inquiry.service.RefundRequestService;
import com.seoulaim.global.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 어드민 환불 관리 (환불 문의). */
@Tag(name = "Admin")
@SecurityRequirement(name = SwaggerConfig.ADMIN_KEY_SCHEME)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/refund-requests")
public class AdminRefundRequestController {

  private final RefundRequestService refundRequestService;

  @Operation(summary = "[관리자] 환불 요청 목록 (처리 대기 오래된 순 → 결정된 것 최신순)")
  @GetMapping
  public List<AdminRefundRequestResponse> getRefundRequests() {
    return refundRequestService.getRefundRequests();
  }

  @Operation(summary = "[관리자] 환불 요청 상세")
  @GetMapping("/{inquiryId}")
  public AdminRefundRequestResponse getRefundRequest(@PathVariable UUID inquiryId) {
    return refundRequestService.getRefundRequest(inquiryId);
  }

  @Operation(summary = "[관리자] 환불 승인 (토스 결제 취소, 정책 밖이면 exception 필수)")
  @PostMapping("/{inquiryId}/approve")
  public AdminRefundRequestResponse approve(
      @PathVariable UUID inquiryId,
      @RequestBody(required = false) AdminRefundApproveRequest request) {
    return refundRequestService.approve(inquiryId, request);
  }

  @Operation(summary = "[관리자] 환불 거절 (사유를 메일로 발송)")
  @PostMapping("/{inquiryId}/reject")
  public AdminRefundRequestResponse reject(
      @PathVariable UUID inquiryId, @RequestBody AdminRefundRejectRequest request) {
    return refundRequestService.reject(inquiryId, request);
  }
}
