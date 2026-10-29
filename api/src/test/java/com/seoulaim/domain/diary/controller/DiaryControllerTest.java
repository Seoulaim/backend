package com.seoulaim.domain.diary.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seoulaim.domain.diary.dto.DiarySubmitResponse;
import com.seoulaim.domain.diary.service.DiaryService;
import com.seoulaim.global.config.SecurityConfig;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DiaryController.class)
@Import(SecurityConfig.class)
class DiaryControllerTest {

  private static final String REQUEST_BODY =
      """
      {
        "orderItemId": "0199a1b2-0000-7000-8000-000000000001",
        "pageCount": 12,
        "content": {"pages": [{"templateId": "t1", "elements": []}]},
        "pdfFileId": "0199a1b2-0000-7000-8000-000000000002"
      }
      """;

  @Autowired private MockMvc mockMvc;
  @MockitoBean private DiaryService diaryService;

  @Test
  @DisplayName("올바른 제출 요청이면 201 과 diaryId 를 반환한다")
  void submitDiary_정상요청_201반환() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID diaryId = UUID.randomUUID();
    given(diaryService.submit(eq(userId), any())).willReturn(new DiarySubmitResponse(diaryId));

    mockMvc
        .perform(
            post("/api/v1/diaries")
                .header("X-User-Id", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.diaryId").value(diaryId.toString()));
  }

  @Test
  @DisplayName("X-User-Id 헤더 없이 제출하면 401 을 반환한다")
  void submitDiary_헤더없음_401반환() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/diaries").contentType(MediaType.APPLICATION_JSON).content(REQUEST_BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));

    verify(diaryService, never()).submit(any(), any());
  }

  @Test
  @DisplayName("pageCount 가 30 을 넘으면 400 과 필드 오류를 반환한다")
  void submitDiary_페이지수초과_400반환() throws Exception {
    String body = REQUEST_BODY.replace("\"pageCount\": 12", "\"pageCount\": 31");

    mockMvc
        .perform(
            post("/api/v1/diaries")
                .header("X-User-Id", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"))
        .andExpect(jsonPath("$.fields.pageCount").exists());

    verify(diaryService, never()).submit(any(), any());
  }

  @Test
  @DisplayName("남의 다이어리 상세를 조회하면 404 DIARY_NOT_FOUND 를 반환한다")
  void getMyDiary_타인다이어리_404반환() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID diaryId = UUID.randomUUID();
    given(diaryService.getMyDiary(userId, diaryId))
        .willThrow(new BusinessException(ErrorCode.DIARY_NOT_FOUND));

    mockMvc
        .perform(get("/api/v1/diaries/{diaryId}", diaryId).header("X-User-Id", userId.toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("DIARY_NOT_FOUND"));
  }

  @Test
  @DisplayName("남의 다이어리 다운로드 URL 을 요청하면 404 DIARY_NOT_FOUND 를 반환한다")
  void getDownloadUrl_타인다이어리_404반환() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID diaryId = UUID.randomUUID();
    given(diaryService.getDownloadUrl(userId, diaryId))
        .willThrow(new BusinessException(ErrorCode.DIARY_NOT_FOUND));

    mockMvc
        .perform(
            get("/api/v1/diaries/{diaryId}/download", diaryId)
                .header("X-User-Id", userId.toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("DIARY_NOT_FOUND"));
  }
}
