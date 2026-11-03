package com.seoulaim.domain.diary.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seoulaim.domain.diary.dto.DiaryCreateResponse;
import com.seoulaim.domain.diary.service.DiaryService;
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
}
