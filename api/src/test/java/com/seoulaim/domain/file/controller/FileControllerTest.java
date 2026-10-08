package com.seoulaim.domain.file.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seoulaim.domain.file.dto.PresignedUploadResponse;
import com.seoulaim.domain.file.service.FileService;
import com.seoulaim.global.config.SecurityConfig;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FileController.class)
@Import(SecurityConfig.class)
class FileControllerTest {

  private static final String REQUEST_BODY =
      """
      {"domain": "REVIEW", "contentType": "image/jpeg", "sizeBytes": 1024}
      """;

  @Autowired private MockMvc mockMvc;
  @MockitoBean private FileService fileService;

  @Test
  @DisplayName("X-User-Id 헤더 없이 presigned 업로드를 요청하면 401 을 반환한다")
  void createPresignedUpload_헤더없음_401반환() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/files/presigned-upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));

    verify(fileService, never()).createPresignedUpload(any(), any());
  }

  @Test
  @DisplayName("X-User-Id 헤더와 올바른 요청이면 200 과 presigned URL 정보를 반환한다")
  void createPresignedUpload_정상요청_200반환() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID fileId = UUID.randomUUID();
    PresignedUploadResponse response =
        new PresignedUploadResponse(
            fileId,
            "review/2026/10/" + fileId + ".jpg",
            "https://seoulaim-dev-591553897454.s3.ap-northeast-2.amazonaws.com/review/signed",
            Instant.parse("2026-10-01T03:10:00Z"));
    given(fileService.createPresignedUpload(eq(userId), any())).willReturn(response);

    mockMvc
        .perform(
            post("/api/v1/files/presigned-upload")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fileId").value(fileId.toString()))
        .andExpect(jsonPath("$.s3Key").value(response.s3Key()))
        .andExpect(jsonPath("$.uploadUrl").value(response.uploadUrl()))
        .andExpect(jsonPath("$.expiresAt").value("2026-10-01T03:10:00Z"));
  }
}
