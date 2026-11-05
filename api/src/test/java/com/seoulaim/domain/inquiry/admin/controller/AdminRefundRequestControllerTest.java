package com.seoulaim.domain.inquiry.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seoulaim.domain.inquiry.admin.dto.AdminRefundRequestResponse;
import com.seoulaim.domain.inquiry.admin.dto.InquiryAuthor;
import com.seoulaim.domain.inquiry.entity.RefundStatus;
import com.seoulaim.domain.inquiry.service.RefundRequestService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminRefundRequestController.class)
@Import(SecurityTestConfig.class)
@TestPropertySource(properties = "app.admin.key=test-admin-key")
class AdminRefundRequestControllerTest {

  private static final String ADMIN_KEY = "test-admin-key";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private RefundRequestService refundRequestService;

  private static AdminRefundRequestResponse requested(UUID id) {
    return new AdminRefundRequestResponse(
        id,
        new InquiryAuthor(UUID.randomUUID(), "Jin", "jin@example.com"),
        "en",
        "Refund for my second diary",
        "Mistake",
        List.of(),
        Instant.parse("2026-11-05T03:00:00Z"),
        new AdminRefundRequestResponse.Refund(RefundStatus.REQUESTED, null, null, null, null),
        new AdminRefundRequestResponse.Payment(
            UUID.randomUUID(),
            "DIA-20261104-ABCDE12345",
            3000L,
            "toss",
            Instant.parse("2026-11-04T03:00:00Z"),
            Instant.parse("2026-11-04T02:59:00Z"),
            null,
            new AdminRefundRequestResponse.Diary(UUID.randomUUID(), "My Diary", false, 4, null)),
        true,
        LocalDate.of(2026, 11, 10));
  }

  @Test
  @DisplayName("환불 요청 목록은 refund.status 소문자, payment.orderId·provider, deadline 을 내려준다")
  void getRefundRequests_형태() throws Exception {
    given(refundRequestService.getRefundRequests())
        .willReturn(List.of(requested(UUID.randomUUID())));

    mockMvc
        .perform(get("/api/v1/admin/refund-requests").header("X-Admin-Key", ADMIN_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].refund.status").value("requested"))
        .andExpect(jsonPath("$[0].refund.exception").doesNotExist())
        .andExpect(jsonPath("$[0].payment.orderId").value("DIA-20261104-ABCDE12345"))
        .andExpect(jsonPath("$[0].payment.provider").value("toss"))
        .andExpect(jsonPath("$[0].payment.diary.first").value(false))
        .andExpect(jsonPath("$[0].payment.diary.pages").value(4))
        .andExpect(jsonPath("$[0].refundWithinPolicy").value(true))
        .andExpect(jsonPath("$[0].deadline").value("2026-11-10"));
  }

  @Test
  @DisplayName("본문 없이 승인할 수 있고, 토스 취소 실패는 502 PAYMENT_REFUND_FAILED 와 토스 code 를 준다")
  void approve_토스실패_502() throws Exception {
    UUID id = UUID.randomUUID();
    given(refundRequestService.approve(eq(id), isNull()))
        .willThrow(
            new BusinessException(
                ErrorCode.PAYMENT_REFUND_FAILED,
                Map.of("code", "NOT_CANCELABLE_PAYMENT", "message", "취소 불가")));

    mockMvc
        .perform(
            post("/api/v1/admin/refund-requests/{id}/approve", id).header("X-Admin-Key", ADMIN_KEY))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value("PAYMENT_REFUND_FAILED"))
        .andExpect(jsonPath("$.fields.code").value("NOT_CANCELABLE_PAYMENT"));
  }

  @Test
  @DisplayName("정책 밖 승인에 exception 이 없으면 400 INQUIRY_POLICY_EXCEPTION_REQUIRED")
  void approve_예외사유없음_400() throws Exception {
    UUID id = UUID.randomUUID();
    given(refundRequestService.approve(eq(id), any()))
        .willThrow(new BusinessException(ErrorCode.INQUIRY_POLICY_EXCEPTION_REQUIRED));

    mockMvc
        .perform(
            post("/api/v1/admin/refund-requests/{id}/approve", id)
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"exception\": null}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INQUIRY_POLICY_EXCEPTION_REQUIRED"));
  }
}
