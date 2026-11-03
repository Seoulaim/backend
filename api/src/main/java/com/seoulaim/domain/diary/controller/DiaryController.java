package com.seoulaim.domain.diary.controller;

import com.seoulaim.domain.diary.dto.DiaryCreateResponse;
import com.seoulaim.domain.diary.dto.DiaryEditorResponse;
import com.seoulaim.domain.diary.dto.DiaryFinishedPagesRequest;
import com.seoulaim.domain.diary.dto.DiaryFinishedPagesResponse;
import com.seoulaim.domain.diary.dto.DiaryNameRequest;
import com.seoulaim.domain.diary.dto.DiaryNewTermsResponse;
import com.seoulaim.domain.diary.dto.DiarySpreadsRequest;
import com.seoulaim.domain.diary.dto.DiarySummaryResponse;
import com.seoulaim.domain.diary.service.DiaryService;
import com.seoulaim.global.common.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

  @Operation(summary = "내 다이어리 목록 (무료이거나 결제된 것만, 최신순)")
  @GetMapping
  public List<DiarySummaryResponse> getMyDiaries(
      @Parameter(hidden = true) @CurrentUserId UUID userId) {
    return diaryService.getMyDiaries(userId);
  }

  @Operation(summary = "새 다이어리 생성 조건 (다음 다이어리 무료 여부, 가격)")
  @GetMapping("/new-terms")
  public DiaryNewTermsResponse getNewTerms(@Parameter(hidden = true) @CurrentUserId UUID userId) {
    return diaryService.getNewTerms(userId);
  }

  @Operation(summary = "다이어리 생성 (첫 다이어리만 무료)")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public DiaryCreateResponse createDiary(@Parameter(hidden = true) @CurrentUserId UUID userId) {
    return diaryService.create(userId);
  }

  @Operation(summary = "다이어리 이름 변경 (1~30자)")
  @PatchMapping("/{diaryId}/name")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void rename(
      @Parameter(hidden = true) @CurrentUserId UUID userId,
      @PathVariable UUID diaryId,
      @RequestBody DiaryNameRequest request) {
    diaryService.rename(userId, diaryId, request.name());
  }

  @Operation(summary = "다이어리 삭제 (soft delete)")
  @DeleteMapping("/{diaryId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @Parameter(hidden = true) @CurrentUserId UUID userId, @PathVariable UUID diaryId) {
    diaryService.delete(userId, diaryId);
  }

  @Operation(summary = "편집 화면 데이터 (편집 데이터, 완성 페이지, 사용 중인 비활성 템플릿)")
  @GetMapping("/{diaryId}/editor")
  public DiaryEditorResponse getEditor(
      @Parameter(hidden = true) @CurrentUserId UUID userId, @PathVariable UUID diaryId) {
    return diaryService.getEditor(userId, diaryId);
  }

  @Operation(summary = "편집 데이터 저장")
  @PutMapping("/{diaryId}/spreads")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void saveSpreads(
      @Parameter(hidden = true) @CurrentUserId UUID userId,
      @PathVariable UUID diaryId,
      @Valid @RequestBody DiarySpreadsRequest request) {
    diaryService.saveSpreads(userId, diaryId, request);
  }

  @Operation(summary = "편집 데이터 저장 후 완성 처리 (이후 편집 불가)")
  @PostMapping("/{diaryId}/finalize")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void finalizeDiary(
      @Parameter(hidden = true) @CurrentUserId UUID userId,
      @PathVariable UUID diaryId,
      @Valid @RequestBody DiarySpreadsRequest request) {
    diaryService.finalizeDiary(userId, diaryId, request);
  }

  @Operation(summary = "완성 페이지 이미지 등록 (멱등. 이미 있으면 기존 key 반환)")
  @PostMapping("/{diaryId}/finished-pages")
  public DiaryFinishedPagesResponse saveFinishedPages(
      @Parameter(hidden = true) @CurrentUserId UUID userId,
      @PathVariable UUID diaryId,
      @Valid @RequestBody DiaryFinishedPagesRequest request) {
    return diaryService.saveFinishedPages(userId, diaryId, request);
  }
}
