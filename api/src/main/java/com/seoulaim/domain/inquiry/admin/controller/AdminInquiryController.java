package com.seoulaim.domain.inquiry.admin.controller;

import com.seoulaim.domain.inquiry.admin.dto.AdminInquiryAnswerRequest;
import com.seoulaim.domain.inquiry.admin.dto.AdminInquiryListResponse;
import com.seoulaim.domain.inquiry.admin.dto.AdminInquiryMemoRequest;
import com.seoulaim.domain.inquiry.admin.dto.AdminInquiryResponse;
import com.seoulaim.domain.inquiry.service.AdminInquiryService;
import com.seoulaim.global.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 어드민 문의 관리 (환불 문의 제외). */
@Tag(name = "Admin")
@SecurityRequirement(name = SwaggerConfig.ADMIN_KEY_SCHEME)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/inquiries")
public class AdminInquiryController {

  private final AdminInquiryService adminInquiryService;

  @Operation(summary = "[관리자] 문의 목록 (환불 제외, 최신순) + 처리 대기 환불 수")
  @GetMapping
  public AdminInquiryListResponse getInquiries() {
    return adminInquiryService.getInquiries();
  }

  @Operation(summary = "[관리자] 문의 상세")
  @GetMapping("/{inquiryId}")
  public AdminInquiryResponse getInquiry(@PathVariable UUID inquiryId) {
    return adminInquiryService.getInquiry(inquiryId);
  }

  @Operation(summary = "[관리자] 답변 (회원 이메일로 발송 후 answered)")
  @PostMapping("/{inquiryId}/answers")
  public AdminInquiryResponse answer(
      @PathVariable UUID inquiryId, @RequestBody AdminInquiryAnswerRequest request) {
    return adminInquiryService.answer(inquiryId, request);
  }

  @Operation(summary = "[관리자] 메모 저장 (회원에게 보내지 않음)")
  @PutMapping("/{inquiryId}/memo")
  public AdminInquiryResponse saveMemo(
      @PathVariable UUID inquiryId, @RequestBody AdminInquiryMemoRequest request) {
    return adminInquiryService.saveMemo(inquiryId, request);
  }
}
