package com.seoulaim.domain.file.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seoulaim.domain.file.dto.PresignedUploadResponse;
import com.seoulaim.domain.file.service.FileService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.security.SecurityTestConfig;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminFileController.class)
@Import(SecurityTestConfig.class)
@TestPropertySource(properties = "app.admin.key=test-admin-key")
class AdminFileControllerTest {

  private static final String ADMIN_KEY = "test-admin-key";
  private static final String URL = "/api/v1/admin/files/presigned-upload";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private FileService fileService;

  private static String body(String domain) {
    return """
        {"domain": "%s", "contentType": "image/png", "sizeBytes": 2048}
        """
        .formatted(domain);
  }

  @Test
  @DisplayName("X-Admin-Key 없이 어드민 업로드를 요청하면 403 AUTH_ACCESS_DENIED 를 반환한다")
  void createPresignedUpload_키없음_403반환() throws Exception {
    mockMvc
        .perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("DIARY_TEMPLATE")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));

    verify(fileService, never()).createAdminPresignedUpload(any());
  }

  @Test
  @DisplayName("허용된 domain 이면 200 과 presigned URL 정보를 반환한다")
  void createPresignedUpload_허용도메인_200반환() throws Exception {
    UUID fileId = UUID.randomUUID();
    PresignedUploadResponse response =
        new PresignedUploadResponse(
            fileId,
            "diary_template/2026/11/" + fileId + ".png",
            "https://example.com/signed",
            Instant.parse("2026-11-03T03:10:00Z"));
    given(fileService.createAdminPresignedUpload(any())).willReturn(response);

    mockMvc
        .perform(
            post(URL)
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("DIARY_TEMPLATE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fileId").value(fileId.toString()))
        .andExpect(jsonPath("$.s3Key").value(response.s3Key()))
        .andExpect(jsonPath("$.uploadUrl").value(response.uploadUrl()))
        .andExpect(jsonPath("$.expiresAt").value("2026-11-03T03:10:00Z"));
  }

  @Test
  @DisplayName("허용되지 않은 domain 이면 400 FILE_INVALID_DOMAIN 을 반환한다")
  void createPresignedUpload_비허용도메인_400반환() throws Exception {
    given(fileService.createAdminPresignedUpload(any()))
        .willThrow(new BusinessException(ErrorCode.FILE_INVALID_DOMAIN));

    mockMvc
        .perform(
            post(URL)
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("REVIEW")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("FILE_INVALID_DOMAIN"));
  }
}
