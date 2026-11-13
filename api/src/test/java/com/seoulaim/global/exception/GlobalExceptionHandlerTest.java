package com.seoulaim.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seoulaim.domain.diary.admin.controller.AdminPaymentController;
import com.seoulaim.domain.diary.service.AdminPaymentService;
import com.seoulaim.global.security.SecurityTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 없는 경로·미지원 메서드가 500 대신 404·405 ErrorResponse 로 응답하는지 확인한다. */
@WebMvcTest(AdminPaymentController.class)
@Import(SecurityTestConfig.class)
@TestPropertySource(properties = "app.admin.key=test-admin-key")
class GlobalExceptionHandlerTest {

  private static final String ADMIN_KEY = "test-admin-key";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private AdminPaymentService adminPaymentService;

  @Test
  @DisplayName("등록되지 않은 경로는 404 COMMON_NOT_FOUND")
  void handleNotFound_없는경로_404() throws Exception {
    mockMvc
        .perform(get("/api/v1/admin/no-such-path").header("X-Admin-Key", ADMIN_KEY))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COMMON_NOT_FOUND"));
  }

  @Test
  @DisplayName("지원하지 않는 메서드는 405 COMMON_METHOD_NOT_ALLOWED 와 Allow 헤더")
  void handleMethodNotSupported_미지원메서드_405() throws Exception {
    mockMvc
        .perform(delete("/api/v1/admin/payments").header("X-Admin-Key", ADMIN_KEY))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().string("Allow", "GET"))
        .andExpect(jsonPath("$.code").value("COMMON_METHOD_NOT_ALLOWED"));
  }
}
