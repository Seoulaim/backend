package com.seoulaim.domain.diary.controller;

import com.seoulaim.domain.diary.dto.DiaryEditorStickersResponse;
import com.seoulaim.domain.diary.dto.DiaryTemplateResponse;
import com.seoulaim.domain.diary.service.DiaryAssetService;
import com.seoulaim.global.common.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Diary")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class DiaryAssetController {

  private final DiaryAssetService diaryAssetService;

  // 로그인 사용자에게만 노출하므로 userId 는 쓰지 않아도 받는다
  @Operation(summary = "다이어리 템플릿 목록 (활성만, 노출 순서)")
  @GetMapping("/diary-templates")
  public List<DiaryTemplateResponse> getTemplates(
      @Parameter(hidden = true) @CurrentUserId UUID userId) {
    return diaryAssetService.getActiveTemplates();
  }

  // #61 에서 배열 → { categories, stickers } 객체로 바뀜 (하위 호환 없음)
  @Operation(summary = "다이어리 스티커 카테고리·스티커 목록 (스티커는 활성만, 둘 다 노출 순서)")
  @GetMapping("/diary-stickers")
  public DiaryEditorStickersResponse getStickers(
      @Parameter(hidden = true) @CurrentUserId UUID userId) {
    return diaryAssetService.getEditorStickers();
  }
}
