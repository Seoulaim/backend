package com.seoulaim.domain.diary.controller;

import com.seoulaim.domain.diary.dto.DiaryStickerResponse;
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

  @Operation(summary = "다이어리 스티커 목록 (활성만, 노출 순서)")
  @GetMapping("/diary-stickers")
  public List<DiaryStickerResponse> getStickers(
      @Parameter(hidden = true) @CurrentUserId UUID userId) {
    return diaryAssetService.getActiveStickers();
  }
}
