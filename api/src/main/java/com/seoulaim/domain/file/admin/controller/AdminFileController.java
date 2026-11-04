package com.seoulaim.domain.file.admin.controller;

import com.seoulaim.domain.file.dto.PresignedUploadRequest;
import com.seoulaim.domain.file.dto.PresignedUploadResponse;
import com.seoulaim.domain.file.service.FileService;
import com.seoulaim.global.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin")
@SecurityRequirement(name = SwaggerConfig.ADMIN_KEY_SCHEME)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/files")
public class AdminFileController {

  private final FileService fileService;

  @Operation(
      summary =
          "[관리자] 이미지 업로드용 presigned PUT URL 발급 (DIARY_TEMPLATE·DIARY_STICKER·PRODUCT·ARTIST·PLACE·CONTENT)")
  @PostMapping("/presigned-upload")
  public PresignedUploadResponse createPresignedUpload(
      @Valid @RequestBody PresignedUploadRequest request) {
    return fileService.createAdminPresignedUpload(request);
  }
}
