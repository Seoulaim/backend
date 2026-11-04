package com.seoulaim.domain.content.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seoulaim.domain.content.admin.dto.AdminBirthdayCafeRequest;
import com.seoulaim.domain.content.admin.dto.AdminBirthdayCafeResponse;
import com.seoulaim.domain.content.admin.dto.AdminCalendarEntryRequest;
import com.seoulaim.domain.content.entity.CalendarColor;
import com.seoulaim.domain.content.service.AdminBannerService;
import com.seoulaim.domain.content.service.AdminBirthdayCafeService;
import com.seoulaim.domain.content.service.AdminCalendarEntryService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.security.SecurityTestConfig;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminContentController.class)
@Import(SecurityTestConfig.class)
@TestPropertySource(properties = "app.admin.key=test-admin-key")
class AdminContentControllerTest {

  private static final String ADMIN_KEY = "test-admin-key";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private AdminCalendarEntryService adminCalendarEntryService;
  @MockitoBean private AdminBirthdayCafeService adminBirthdayCafeService;
  @MockitoBean private AdminBannerService adminBannerService;

  @Test
  @DisplayName("기본 배너 끄기는 409 CONTENT_DEFAULT_BANNER")
  void changeBannerEnabled_기본배너_409() throws Exception {
    UUID id = UUID.randomUUID();
    willThrow(new BusinessException(ErrorCode.CONTENT_DEFAULT_BANNER))
        .given(adminBannerService)
        .changeEnabled(id, false);

    mockMvc
        .perform(
            patch("/api/v1/admin/banners/{id}/enabled", id)
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\": false}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONTENT_DEFAULT_BANNER"));
  }

  @Test
  @DisplayName("X-Admin-Key 없으면 403")
  void deleteBanner_키없음_403() throws Exception {
    mockMvc
        .perform(delete("/api/v1/admin/banners/{id}", UUID.randomUUID()))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("생일카페는 어드민 mock 의 memberId 로 보내도 artistId 로 받아 201 을 반환한다")
  void createBirthdayCafe_memberId별칭_201() throws Exception {
    UUID memberId = UUID.randomUUID();
    ArgumentCaptor<AdminBirthdayCafeRequest> captor =
        ArgumentCaptor.forClass(AdminBirthdayCafeRequest.class);
    given(adminBirthdayCafeService.createCafe(any()))
        .willReturn(
            new AdminBirthdayCafeResponse(
                UUID.randomUUID(),
                memberId,
                Map.of("en", "Cafe"),
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 12),
                null,
                null,
                0,
                Instant.parse("2026-11-04T00:00:00Z")));

    mockMvc
        .perform(
            post("/api/v1/admin/birthday-cafes")
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"memberId": "%s", "venue": {"en": "Cafe"},
                     "startDate": "2026-11-10", "endDate": "2026-11-12"}
                    """
                        .formatted(memberId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.artistId").value(memberId.toString()));

    verify(adminBirthdayCafeService).createCafe(captor.capture());
    assertThat(captor.getValue().artistId()).isEqualTo(memberId);
  }

  @Test
  @DisplayName("캘린더 항목 color 는 소문자로 받는다")
  void createCalendarEntry_소문자색_201() throws Exception {
    given(adminCalendarEntryService.createEntry(any())).willReturn(null);

    mockMvc
        .perform(
            post("/api/v1/admin/calendar-entries")
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"groupIds": ["all"], "title": {"en": "Festival"},
                     "startDate": "2026-11-10", "inCalendar": true, "color": "green"}
                    """))
        .andExpect(status().isCreated());

    verify(adminCalendarEntryService)
        .createEntry(
            eq(
                new AdminCalendarEntryRequest(
                    List.of("all"),
                    Map.of("en", "Festival"),
                    LocalDate.of(2026, 11, 10),
                    true,
                    CalendarColor.GREEN,
                    null,
                    null,
                    null,
                    null,
                    null)));
  }
}
