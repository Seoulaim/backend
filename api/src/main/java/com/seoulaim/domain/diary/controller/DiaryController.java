package com.seoulaim.domain.diary.controller;

import com.seoulaim.domain.diary.dto.DiaryDetailResponse;
import com.seoulaim.domain.diary.dto.DiaryDownloadResponse;
import com.seoulaim.domain.diary.dto.DiaryPendingOrderResponse;
import com.seoulaim.domain.diary.dto.DiarySubmitRequest;
import com.seoulaim.domain.diary.dto.DiarySubmitResponse;
import com.seoulaim.domain.diary.dto.DiarySummaryResponse;
import com.seoulaim.domain.diary.service.DiaryService;
import com.seoulaim.global.common.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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

@Tag(name = "Diary")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/diaries")
public class DiaryController {

  private final DiaryService diaryService;

  @Operation(summary = "다이어리 제출 가능한 주문 (결제 완료·미제출 다이어리 상품)")
  @Parameter(
      name = "X-User-Id",
      in = ParameterIn.HEADER,
      required = true,
      description = "auth 구현 전 임시 유저 식별 헤더 (UUID)")
  @GetMapping("/pending-orders")
  public List<DiaryPendingOrderResponse> getPendingOrders(
      @Parameter(hidden = true) @CurrentUserId UUID userId) {
    return diaryService.getPendingOrders(userId);
  }

  @Operation(summary = "다이어리 제출 (클라이언트가 만든 PDF 를 업로드한 뒤 호출)")
  @Parameter(
      name = "X-User-Id",
      in = ParameterIn.HEADER,
      required = true,
      description = "auth 구현 전 임시 유저 식별 헤더 (UUID)")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DiarySubmitResponse submitDiary(
      @Parameter(hidden = true) @CurrentUserId UUID userId,
      @Valid @RequestBody DiarySubmitRequest request) {
    return diaryService.submit(userId, request);
  }

  @Operation(summary = "내 다이어리 목록 (최신순)")
  @Parameter(
      name = "X-User-Id",
      in = ParameterIn.HEADER,
      required = true,
      description = "auth 구현 전 임시 유저 식별 헤더 (UUID)")
  @GetMapping
  public List<DiarySummaryResponse> getMyDiaries(
      @Parameter(hidden = true) @CurrentUserId UUID userId) {
    return diaryService.getMyDiaries(userId);
  }

  @Operation(summary = "내 다이어리 상세 (편집 데이터 포함)")
  @Parameter(
      name = "X-User-Id",
      in = ParameterIn.HEADER,
      required = true,
      description = "auth 구현 전 임시 유저 식별 헤더 (UUID)")
  @GetMapping("/{diaryId}")
  public DiaryDetailResponse getMyDiary(
      @Parameter(hidden = true) @CurrentUserId UUID userId, @PathVariable UUID diaryId) {
    return diaryService.getMyDiary(userId, diaryId);
  }

  @Operation(summary = "다이어리 PDF 다운로드용 presigned GET URL 발급 (10분)")
  @Parameter(
      name = "X-User-Id",
      in = ParameterIn.HEADER,
      required = true,
      description = "auth 구현 전 임시 유저 식별 헤더 (UUID)")
  @GetMapping("/{diaryId}/download")
  public DiaryDownloadResponse getDownloadUrl(
      @Parameter(hidden = true) @CurrentUserId UUID userId, @PathVariable UUID diaryId) {
    return diaryService.getDownloadUrl(userId, diaryId);
  }
}
