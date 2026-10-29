package com.seoulaim.domain.diary.admin.controller;

import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerRequest;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerResponse;
import com.seoulaim.domain.diary.admin.dto.AdminDiarySummaryResponse;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryTemplateRequest;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryTemplateResponse;
import com.seoulaim.domain.diary.service.DiaryAssetService;
import com.seoulaim.domain.diary.service.DiaryService;
import com.seoulaim.global.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin")
@SecurityRequirement(name = SwaggerConfig.ADMIN_KEY_SCHEME)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminDiaryController {

  private final DiaryAssetService diaryAssetService;
  private final DiaryService diaryService;

  @Operation(summary = "[관리자] 다이어리 템플릿 목록 (비활성 포함)")
  @GetMapping("/diary-templates")
  public List<AdminDiaryTemplateResponse> getTemplates() {
    return diaryAssetService.getAllTemplates();
  }

  @Operation(summary = "[관리자] 다이어리 템플릿 등록")
  @PostMapping("/diary-templates")
  @ResponseStatus(HttpStatus.CREATED)
  public AdminDiaryTemplateResponse createTemplate(
      @Valid @RequestBody AdminDiaryTemplateRequest request) {
    return diaryAssetService.createTemplate(request);
  }

  @Operation(summary = "[관리자] 다이어리 템플릿 수정")
  @PutMapping("/diary-templates/{templateId}")
  public AdminDiaryTemplateResponse updateTemplate(
      @PathVariable UUID templateId, @Valid @RequestBody AdminDiaryTemplateRequest request) {
    return diaryAssetService.updateTemplate(templateId, request);
  }

  @Operation(summary = "[관리자] 다이어리 스티커 목록 (비활성 포함)")
  @GetMapping("/diary-stickers")
  public List<AdminDiaryStickerResponse> getStickers() {
    return diaryAssetService.getAllStickers();
  }

  @Operation(summary = "[관리자] 다이어리 스티커 등록")
  @PostMapping("/diary-stickers")
  @ResponseStatus(HttpStatus.CREATED)
  public AdminDiaryStickerResponse createSticker(
      @Valid @RequestBody AdminDiaryStickerRequest request) {
    return diaryAssetService.createSticker(request);
  }

  @Operation(summary = "[관리자] 다이어리 스티커 수정")
  @PutMapping("/diary-stickers/{stickerId}")
  public AdminDiaryStickerResponse updateSticker(
      @PathVariable UUID stickerId, @Valid @RequestBody AdminDiaryStickerRequest request) {
    return diaryAssetService.updateSticker(stickerId, request);
  }

  @Operation(summary = "[관리자] 제출된 다이어리 목록 (최신순, 최대 200개)")
  @GetMapping("/diaries")
  public List<AdminDiarySummaryResponse> getDiaries() {
    return diaryService.getAdminDiaries();
  }
}
