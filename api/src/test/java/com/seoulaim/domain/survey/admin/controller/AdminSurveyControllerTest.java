package com.seoulaim.domain.survey.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seoulaim.domain.survey.admin.dto.AdminSurveyEnabledResponse;
import com.seoulaim.domain.survey.service.AdminSurveyService;
import com.seoulaim.global.security.SecurityTestConfig;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminSurveyController.class)
@Import(SecurityTestConfig.class)
@TestPropertySource(properties = "app.admin.key=test-admin-key")
class AdminSurveyControllerTest {

  private static final String ADMIN_KEY = "test-admin-key";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private AdminSurveyService adminSurveyService;

  @Test
  @DisplayName("팝업을 켜면 { enabled: true } 를 반환한다")
  void setEnabled_켜기_200반환() throws Exception {
    given(adminSurveyService.setEnabled(true)).willReturn(new AdminSurveyEnabledResponse(true));

    mockMvc
        .perform(
            patch("/api/v1/admin/survey/enabled")
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\": true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(true));
  }

  @Test
  @DisplayName("결과 조회의 날짜가 YYYY-MM-DD 가 아니면 400 을 반환한다")
  void getResults_날짜형식오류_400반환() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/admin/survey/responses")
                .header("X-Admin-Key", ADMIN_KEY)
                .param("from", "2026/11/01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"));

    verify(adminSurveyService, never()).getResults(any(LocalDate.class), any());
  }

  @Test
  @DisplayName("X-Admin-Key 없이 호출하면 403 을 반환한다")
  void getSurvey_키없음_403반환() throws Exception {
    mockMvc
        .perform(get("/api/v1/admin/survey"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));

    verify(adminSurveyService, never()).getSurvey();
  }
}
