package com.seoulaim.domain.inquiry.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seoulaim.domain.inquiry.admin.dto.AdminInquiryAnswerResponse;
import com.seoulaim.domain.inquiry.admin.dto.AdminInquiryListResponse;
import com.seoulaim.domain.inquiry.admin.dto.AdminInquiryResponse;
import com.seoulaim.domain.inquiry.admin.dto.InquiryAuthor;
import com.seoulaim.domain.inquiry.entity.InquiryCategory;
import com.seoulaim.domain.inquiry.entity.InquiryStatus;
import com.seoulaim.domain.inquiry.service.AdminInquiryService;
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

@WebMvcTest(AdminInquiryController.class)
@Import(SecurityTestConfig.class)
@TestPropertySource(properties = "app.admin.key=test-admin-key")
class AdminInquiryControllerTest {

  private static final String ADMIN_KEY = "test-admin-key";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private AdminInquiryService adminInquiryService;

  private static AdminInquiryResponse inquiry(UUID id, InquiryStatus status) {
    return new AdminInquiryResponse(
        id,
        new InquiryAuthor(UUID.randomUUID(), "Hana", "hana@example.com"),
        "en",
        InquiryCategory.PAYMENT,
        "Charged twice?",
        "Two charges",
        List.of("inquiry/2026/11/a.jpg"),
        Instant.parse("2026-11-05T01:00:00Z"),
        status,
        status == InquiryStatus.ANSWERED
            ? List.of(
                new AdminInquiryAnswerResponse(
                    UUID.randomUUID(), "Sorry", Instant.parse("2026-11-05T02:00:00Z"), "운영자"))
            : List.of(),
        null);
  }

  @Test
  @DisplayName("목록은 category·status 를 소문자로 내려주고 refundCount 를 포함한다")
  void getInquiries_소문자값() throws Exception {
    given(adminInquiryService.getInquiries())
        .willReturn(
            new AdminInquiryListResponse(
                List.of(inquiry(UUID.randomUUID(), InquiryStatus.RECEIVED)), 3));

    mockMvc
        .perform(get("/api/v1/admin/inquiries").header("X-Admin-Key", ADMIN_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.refundCount").value(3))
        .andExpect(jsonPath("$.inquiries[0].category").value("payment"))
        .andExpect(jsonPath("$.inquiries[0].status").value("received"))
        .andExpect(jsonPath("$.inquiries[0].author.nickname").value("Hana"))
        .andExpect(jsonPath("$.inquiries[0].photoKeys[0]").value("inquiry/2026/11/a.jpg"));
  }

  @Test
  @DisplayName("답변은 갱신된 문의(answered, answers)를 반환한다")
  void answer_갱신된문의() throws Exception {
    UUID id = UUID.randomUUID();
    given(adminInquiryService.answer(eq(id), any()))
        .willReturn(inquiry(id, InquiryStatus.ANSWERED));

    mockMvc
        .perform(
            post("/api/v1/admin/inquiries/{id}/answers", id)
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Sorry\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("answered"))
        .andExpect(jsonPath("$.answers[0].adminName").value("운영자"));
  }

  @Test
  @DisplayName("X-Admin-Key 가 없으면 403")
  void getInquiries_키없음_403() throws Exception {
    mockMvc.perform(get("/api/v1/admin/inquiries")).andExpect(status().isForbidden());
  }
}
