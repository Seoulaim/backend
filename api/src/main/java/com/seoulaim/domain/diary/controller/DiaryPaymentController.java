package com.seoulaim.domain.diary.controller;

import com.seoulaim.domain.diary.dto.DiaryCheckoutResponse;
import com.seoulaim.domain.diary.dto.DiaryPaymentConfirmRequest;
import com.seoulaim.domain.diary.dto.DiaryPaymentConfirmResponse;
import com.seoulaim.domain.diary.dto.DiaryPaymentCreateRequest;
import com.seoulaim.domain.diary.dto.DiaryPaymentCreateResponse;
import com.seoulaim.domain.diary.dto.MyPaymentResponse;
import com.seoulaim.domain.diary.service.DiaryPaymentService;
import com.seoulaim.global.common.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Diary Payment")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class DiaryPaymentController {

  private final DiaryPaymentService diaryPaymentService;

  @Operation(summary = "결제 화면 정보")
  @GetMapping("/diaries/{diaryId}/checkout")
  public DiaryCheckoutResponse getCheckout(
      @Parameter(hidden = true) @CurrentUserId UUID userId, @PathVariable UUID diaryId) {
    return diaryPaymentService.getCheckout(userId, diaryId);
  }

  @Operation(summary = "결제 대기 건 생성 (토스 결제창 호출 전)")
  @PostMapping("/diaries/{diaryId}/payments")
  @ResponseStatus(HttpStatus.CREATED)
  public DiaryPaymentCreateResponse createPayment(
      @Parameter(hidden = true) @CurrentUserId UUID userId,
      @PathVariable UUID diaryId,
      @RequestBody DiaryPaymentCreateRequest request) {
    return diaryPaymentService.createPayment(userId, diaryId, request);
  }

  @Operation(summary = "토스 결제 승인 (멱등)")
  @PostMapping("/payments/{orderId}/confirm")
  public DiaryPaymentConfirmResponse confirm(
      @Parameter(hidden = true) @CurrentUserId UUID userId,
      @PathVariable UUID orderId,
      @Valid @RequestBody DiaryPaymentConfirmRequest request) {
    return diaryPaymentService.confirm(userId, orderId, request);
  }

  @Operation(summary = "결제 후 편집 이력 기록 (환불 정책 판단용)")
  @PostMapping("/diaries/{diaryId}/edited")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void markEdited(
      @Parameter(hidden = true) @CurrentUserId UUID userId, @PathVariable UUID diaryId) {
    diaryPaymentService.markEdited(userId, diaryId);
  }

  @Operation(summary = "내 결제 내역 (최신순)")
  @GetMapping("/me/payments")
  public List<MyPaymentResponse> getMyPayments(
      @Parameter(hidden = true) @CurrentUserId UUID userId) {
    return diaryPaymentService.getMyPayments(userId);
  }
}
