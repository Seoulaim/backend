package com.seoulaim.domain.inquiry.controller;

import com.seoulaim.domain.inquiry.dto.InquiryCreateRequest;
import com.seoulaim.domain.inquiry.dto.InquiryCreateResponse;
import com.seoulaim.domain.inquiry.service.InquiryService;
import com.seoulaim.global.common.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Inquiry")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/inquiries")
public class InquiryController {

  private final InquiryService inquiryService;

  @Operation(summary = "문의 작성 (환불 문의는 paymentId 필수)")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public InquiryCreateResponse submit(
      @Parameter(hidden = true) @CurrentUserId UUID userId,
      @RequestBody InquiryCreateRequest request) {
    return inquiryService.submit(userId, request);
  }
}
