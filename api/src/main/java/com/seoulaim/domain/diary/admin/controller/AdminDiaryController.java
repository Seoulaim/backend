package com.seoulaim.domain.diary.admin.controller;

import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerCategoryListResponse;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerCategoryReorderRequest;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerCategoryRequest;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerCategoryResponse;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerRequest;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerResponse;
import com.seoulaim.domain.diary.admin.dto.AdminDiarySummaryResponse;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryTemplateRequest;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryTemplateResponse;
import com.seoulaim.domain.diary.service.DiaryAssetService;
import com.seoulaim.domain.diary.service.DiaryService;
import com.seoulaim.domain.diary.service.DiaryStickerCategoryService;
import com.seoulaim.global.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
  private final DiaryStickerCategoryService diaryStickerCategoryService;

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

  @Operation(summary = "[관리자] 스티커 카테고리 목록 (노출 순서, 활성 스티커 수 포함)")
  @GetMapping("/diary-sticker-categories")
  public List<AdminDiaryStickerCategoryListResponse> getStickerCategories() {
    return diaryStickerCategoryService.getCategories();
  }

  @Operation(summary = "[관리자] 스티커 카테고리 등록 (맨 뒤)")
  @PostMapping("/diary-sticker-categories")
  @ResponseStatus(HttpStatus.CREATED)
  public AdminDiaryStickerCategoryResponse createStickerCategory(
      @RequestBody AdminDiaryStickerCategoryRequest request) {
    return diaryStickerCategoryService.createCategory(request.name());
  }

  @Operation(summary = "[관리자] 스티커 카테고리 순서 변경 (빠진 카테고리는 뒤로)")
  @PutMapping("/diary-sticker-categories/reorder")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void reorderStickerCategories(
      @Valid @RequestBody AdminDiaryStickerCategoryReorderRequest request) {
    diaryStickerCategoryService.reorderCategories(request.ids());
  }

  @Operation(summary = "[관리자] 스티커 카테고리 이름 수정")
  @PutMapping("/diary-sticker-categories/{categoryId}")
  public AdminDiaryStickerCategoryResponse updateStickerCategory(
      @PathVariable UUID categoryId, @RequestBody AdminDiaryStickerCategoryRequest request) {
    return diaryStickerCategoryService.updateCategory(categoryId, request.name());
  }

  @Operation(summary = "[관리자] 스티커 카테고리 삭제 (매핑만 삭제, 스티커는 유지)")
  @DeleteMapping("/diary-sticker-categories/{categoryId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteStickerCategory(@PathVariable UUID categoryId) {
    diaryStickerCategoryService.deleteCategory(categoryId);
  }

  @Operation(summary = "[관리자] 다이어리 목록 (삭제 제외, 생성 최신순, 최대 200개)")
  @GetMapping("/diaries")
  public List<AdminDiarySummaryResponse> getDiaries() {
    return diaryService.getAdminDiaries();
  }
}
