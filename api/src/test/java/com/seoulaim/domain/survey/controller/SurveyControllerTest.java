package com.seoulaim.domain.survey.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seoulaim.domain.survey.dto.SurveyEventType;
import com.seoulaim.domain.survey.dto.SurveyInvite;
import com.seoulaim.domain.survey.dto.TravelVisitResponse;
import com.seoulaim.domain.survey.service.SurveyService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.security.SecurityTestConfig;
import com.seoulaim.global.security.TestAccessTokens;
import java.util.Map;
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

@WebMvcTest(SurveyController.class)
@Import(SecurityTestConfig.class)
class SurveyControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private TestAccessTokens tokens;
  @MockitoBean private SurveyService surveyService;

  @Test
  @DisplayName("TRAVEL 방문 기록은 초대 문구를 surveyInvite 로 감싸 반환한다")
  void recordTravelVisit_초대있음_200반환() throws Exception {
    UUID userId = UUID.randomUUID();
    given(surveyService.recordTravelVisit(userId))
        .willReturn(
            new TravelVisitResponse(
                new SurveyInvite(Map.of("en", "How was it?"), Map.of("en", "Gift card"))));

    mockMvc
        .perform(
            post("/api/v1/me/travel-visits")
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(userId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.surveyInvite.title.en").value("How was it?"));
  }

  @Test
  @DisplayName("앱이 보내는 소문자 이벤트(snoozed)도 받아 204 를 반환한다")
  void recordEvent_소문자_204반환() throws Exception {
    UUID userId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/api/v1/me/plan-survey/events")
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\": \"snoozed\"}"))
        .andExpect(status().isNoContent());

    verify(surveyService).recordEvent(userId, SurveyEventType.SNOOZED);
  }

  @Test
  @DisplayName("모르는 이벤트 type 이면 400 을 반환한다")
  void recordEvent_모르는타입_400반환() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/me/plan-survey/events")
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(UUID.randomUUID()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\": \"clicked\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"));

    verify(surveyService, never()).recordEvent(any(), any());
  }

  @Test
  @DisplayName("이미 응답했으면 409 SURVEY_ALREADY_ANSWERED 를 반환한다")
  void submit_이미응답_409반환() throws Exception {
    UUID userId = UUID.randomUUID();
    willThrow(new BusinessException(ErrorCode.SURVEY_ALREADY_ANSWERED))
        .given(surveyService)
        .submit(eq(userId), anyMap());

    mockMvc
        .perform(
            post("/api/v1/me/plan-survey")
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answers\": {\"q\": 5}}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("SURVEY_ALREADY_ANSWERED"));
  }

  @Test
  @DisplayName("access token 없이 방문을 기록하면 401 을 반환한다")
  void recordTravelVisit_토큰없음_401반환() throws Exception {
    mockMvc.perform(post("/api/v1/me/travel-visits")).andExpect(status().isUnauthorized());

    verify(surveyService, never()).recordTravelVisit(any());
  }
}
