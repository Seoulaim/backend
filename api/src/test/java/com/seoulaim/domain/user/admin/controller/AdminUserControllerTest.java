package com.seoulaim.domain.user.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seoulaim.domain.user.admin.dto.AdminUserListResponse;
import com.seoulaim.domain.user.service.AdminUserService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.security.SecurityTestConfig;
import java.time.Instant;
import java.util.List;
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

@WebMvcTest(AdminUserController.class)
@Import(SecurityTestConfig.class)
@TestPropertySource(properties = "app.admin.key=test-admin-key")
class AdminUserControllerTest {

  private static final String ADMIN_KEY = "test-admin-key";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private AdminUserService adminUserService;

  @Test
  @DisplayName("회원 목록은 emailMasked·소문자 status 와 페이지 정보를 내려준다")
  void getUsers_형태() throws Exception {
    UUID id = UUID.randomUUID();
    given(adminUserService.getUsers("mina", null, 0, 50))
        .willReturn(
            new AdminUserListResponse(
                List.of(
                    new AdminUserListResponse.Item(
                        id,
                        "Mina",
                        "mi**@example.com",
                        "en",
                        Instant.parse("2026-10-01T00:00:00Z"),
                        1,
                        1,
                        "active")),
                0,
                50,
                1));

    mockMvc
        .perform(get("/api/v1/admin/users").param("q", "mina").header("X-Admin-Key", ADMIN_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].emailMasked").value("mi**@example.com"))
        .andExpect(jsonPath("$.items[0].email").doesNotExist())
        .andExpect(jsonPath("$.items[0].status").value("active"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  @DisplayName("강퇴는 204, 이미 탈퇴면 409 USER_ALREADY_WITHDRAWN, 어드민 키 없으면 403")
  void withdraw_응답() throws Exception {
    UUID id = UUID.randomUUID();
    UUID withdrawn = UUID.randomUUID();
    willThrow(new BusinessException(ErrorCode.USER_ALREADY_WITHDRAWN))
        .given(adminUserService)
        .withdraw(withdrawn, "abuse");

    mockMvc
        .perform(
            post("/api/v1/admin/users/{id}/withdraw", id)
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"abuse\"}"))
        .andExpect(status().isNoContent());
    verify(adminUserService).withdraw(id, "abuse");
    mockMvc
        .perform(
            post("/api/v1/admin/users/{id}/withdraw", withdrawn)
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"abuse\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("USER_ALREADY_WITHDRAWN"));
    mockMvc
        .perform(
            post("/api/v1/admin/users/{id}/withdraw", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"abuse\"}"))
        .andExpect(status().isForbidden());
    verify(adminUserService, never()).getUser(any());
  }
}
