package com.seoulaim.domain.content.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seoulaim.domain.content.dto.FanScheduleResponse;
import com.seoulaim.domain.content.entity.CalendarColor;
import com.seoulaim.domain.content.service.HomeContentService;
import com.seoulaim.global.security.SecurityTestConfig;
import com.seoulaim.global.security.TestAccessTokens;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HomeContentController.class)
@Import(SecurityTestConfig.class)
class HomeContentControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private TestAccessTokens tokens;
  @MockitoBean private HomeContentService homeContentService;

  @Test
  @DisplayName("토큰 없이 호출하면 200 이고 userId 는 null 로 전달된다 (최애 필터 없음)")
  void getUpcomingEvents_비로그인_userIdNull() throws Exception {
    given(homeContentService.getUpcomingEvents(isNull())).willReturn(List.of());

    mockMvc.perform(get("/api/v1/home/events")).andExpect(status().isOk());

    verify(homeContentService).getUpcomingEvents(null);
  }

  @Test
  @DisplayName("유효한 토큰이 있으면 그 userId 로 최애 필터를 적용한다")
  void getBirthdayCafes_로그인_userId전달() throws Exception {
    UUID userId = UUID.randomUUID();
    given(homeContentService.getBirthdayCafes(userId)).willReturn(List.of());

    mockMvc
        .perform(
            get("/api/v1/home/birthday-cafes")
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(userId)))
        .andExpect(status().isOk());

    verify(homeContentService).getBirthdayCafes(userId);
  }

  @Test
  @DisplayName("토큰이 있는데 위조·만료면 비로그인으로 넘기지 않고 401 AUTH_INVALID_TOKEN 을 반환한다")
  void getUpcomingEvents_잘못된토큰_401() throws Exception {
    mockMvc
        .perform(get("/api/v1/home/events").header(HttpHeaders.AUTHORIZATION, "Bearer broken"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));

    verifyNoInteractions(homeContentService);
  }

  @Test
  @DisplayName("팬캘린더는 date·소문자 color 로 응답하고, from/to 가 없으면 400 이다")
  void getFanSchedules_응답형식과필수파라미터() throws Exception {
    LocalDate date = LocalDate.of(2026, 11, 7);
    given(homeContentService.getFanSchedules(isNull(), any(), any()))
        .willReturn(
            List.of(
                new FanScheduleResponse(
                    UUID.randomUUID(), date, Map.of("en", "ATEEZ TOUR"), CalendarColor.PURPLE)));

    mockMvc
        .perform(get("/api/v1/home/fan-schedules?from=2026-11-01&to=2026-11-30"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].date").value("2026-11-07"))
        .andExpect(jsonPath("$[0].color").value("purple"));

    mockMvc
        .perform(get("/api/v1/home/fan-schedules?from=2026-11-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"));
  }

  @Test
  @DisplayName("다이어리 배너는 비로그인으로 조회할 수 있다")
  void getDiaryBanners_비로그인_200() throws Exception {
    given(homeContentService.getDiaryBanners()).willReturn(List.of());

    mockMvc.perform(get("/api/v1/diary-banners")).andExpect(status().isOk());
  }
}
