package com.seoulaim.domain.diary.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seoulaim.domain.diary.dto.DiaryCreateResponse;
import com.seoulaim.domain.diary.service.DiaryService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.security.SecurityTestConfig;
import com.seoulaim.global.security.TestAccessTokens;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DiaryController.class)
@Import(SecurityTestConfig.class)
class DiaryControllerTest {

  private static final String SPREADS_BODY =
      """
      {"spreads": [{"left": {}, "right": {}}], "placePhotos": {}}
      """;

  @Autowired private MockMvc mockMvc;
  @Autowired private TestAccessTokens tokens;
  @MockitoBean private DiaryService diaryService;

  @Test
  @DisplayName("다이어리를 만들면 201 과 diaryId 를 반환한다")
  void createDiary_정상_201반환() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID diaryId = UUID.randomUUID();
    given(diaryService.create(userId)).willReturn(new DiaryCreateResponse(diaryId));

    mockMvc
        .perform(post("/api/v1/diaries").header(HttpHeaders.AUTHORIZATION, tokens.bearer(userId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.diaryId").value(diaryId.toString()));
  }

  @Test
  @DisplayName("access token 없이 목록을 조회하면 401 을 반환한다")
  void getMyDiaries_토큰없음_401반환() throws Exception {
    mockMvc
        .perform(get("/api/v1/diaries"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));

    verify(diaryService, never()).getMyDiaries(any());
  }

  @Test
  @DisplayName("편집 저장이 무료 범위를 넘으면 402 DIARY_PAYMENT_REQUIRED 를 반환한다")
  void saveSpreads_결제필요_402반환() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID diaryId = UUID.randomUUID();
    willThrow(new BusinessException(ErrorCode.DIARY_PAYMENT_REQUIRED))
        .given(diaryService)
        .saveSpreads(eq(userId), eq(diaryId), any());

    mockMvc
        .perform(
            put("/api/v1/diaries/{diaryId}/spreads", diaryId)
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(SPREADS_BODY))
        .andExpect(status().isPaymentRequired())
        .andExpect(jsonPath("$.code").value("DIARY_PAYMENT_REQUIRED"));
  }

  @Test
  @DisplayName("placePhotos 를 빠뜨리면 400 과 필드 오류를 반환한다")
  void saveSpreads_placePhotos없음_400반환() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/diaries/{diaryId}/spreads", UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(UUID.randomUUID()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"spreads\": []}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fields.placePhotos").exists());

    verify(diaryService, never()).saveSpreads(any(), any(), any());
  }

  @Test
  @DisplayName("완성된 다이어리를 다시 finalize 하면 409 DIARY_FINALIZED 를 반환한다")
  void finalizeDiary_완성됨_409반환() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID diaryId = UUID.randomUUID();
    willThrow(new BusinessException(ErrorCode.DIARY_FINALIZED))
        .given(diaryService)
        .finalizeDiary(eq(userId), eq(diaryId), any());

    mockMvc
        .perform(
            post("/api/v1/diaries/{diaryId}/finalize", diaryId)
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(SPREADS_BODY))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DIARY_FINALIZED"));
  }

  @Test
  @DisplayName("이름 변경이 성공하면 204 를 반환한다")
  void rename_정상_204반환() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID diaryId = UUID.randomUUID();

    mockMvc
        .perform(
            patch("/api/v1/diaries/{diaryId}/name", diaryId)
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"Seoul Trip\"}"))
        .andExpect(status().isNoContent());

    verify(diaryService).rename(userId, diaryId, "Seoul Trip");
  }

  @Test
  @DisplayName("남의 다이어리 편집 화면을 조회하면 404 DIARY_NOT_FOUND 를 반환한다")
  void getEditor_타인다이어리_404반환() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID diaryId = UUID.randomUUID();
    given(diaryService.getEditor(userId, diaryId))
        .willThrow(new BusinessException(ErrorCode.DIARY_NOT_FOUND));

    mockMvc
        .perform(
            get("/api/v1/diaries/{diaryId}/editor", diaryId)
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(userId)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("DIARY_NOT_FOUND"));
  }

  @Test
  @DisplayName("fileIds 가 비어 있으면 finished-pages 는 400 을 반환한다")
  void saveFinishedPages_빈배열_400반환() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/diaries/{diaryId}/finished-pages", UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(UUID.randomUUID()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fileIds\": []}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fields.fileIds").exists());
  }
}
