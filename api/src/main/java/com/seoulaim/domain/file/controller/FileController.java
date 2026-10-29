package com.seoulaim.domain.file.controller;

import com.seoulaim.domain.file.dto.FileResponse;
import com.seoulaim.domain.file.dto.PresignedUploadRequest;
import com.seoulaim.domain.file.dto.PresignedUploadResponse;
import com.seoulaim.domain.file.service.FileService;
import com.seoulaim.global.common.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "File")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileController {

  private final FileService fileService;

  @Operation(summary = "이미지(다이어리는 PDF 포함) 업로드용 presigned PUT URL 발급")
  @Parameter(
      name = "X-User-Id",
      in = ParameterIn.HEADER,
      required = true,
      description = "auth 구현 전 임시 유저 식별 헤더 (UUID)")
  @PostMapping("/presigned-upload")
  public PresignedUploadResponse createPresignedUpload(
      @Parameter(hidden = true) @CurrentUserId UUID userId,
      @Valid @RequestBody PresignedUploadRequest request) {
    return fileService.createPresignedUpload(userId, request);
  }

  @Operation(summary = "파일 상태 조회 (디버깅용)")
  @GetMapping("/{fileId}")
  public FileResponse getFile(@PathVariable UUID fileId) {
    return fileService.getById(fileId);
  }
}
