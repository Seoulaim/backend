package com.seoulaim.domain.diary.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seoulaim.domain.diary.admin.dto.AdminPaymentDetailResponse;
import com.seoulaim.domain.diary.admin.dto.AdminPaymentPageResponse;
import com.seoulaim.domain.diary.admin.dto.AdminPaymentResponse;
import com.seoulaim.domain.diary.entity.DiaryPaymentMethod;
import com.seoulaim.domain.diary.service.AdminPaymentService;
import com.seoulaim.domain.order.gateway.PaymentCurrency;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.security.SecurityTestConfig;
import java.math.BigDecimal;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminPaymentController.class)
@Import(SecurityTestConfig.class)
@TestPropertySource(properties = "app.admin.key=test-admin-key")
class AdminPaymentControllerTest {

  private static final String ADMIN_KEY = "test-admin-key";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private AdminPaymentService adminPaymentService;

  private static AdminPaymentResponse item(UUID id) {
    return new AdminPaymentResponse(
        id,
        "DIA-20261104-ABCDE12345",
        new AdminPaymentResponse.Member(UUID.randomUUID(), "Jin", "jin@example.com"),
        new AdminPaymentResponse.Diary(UUID.randomUUID(), "My Diary", false, 4, null, false),
        new BigDecimal("3000"),
        PaymentCurrency.KRW,
        "toss",
        "paid",
        Instant.parse("2026-11-04T02:59:00Z"),
        Instant.parse("2026-11-04T03:00:00Z"),
        null,
        Instant.parse("2026-11-04T02:59:00Z"),
        null,
        false,
        null,
        DiaryPaymentMethod.CARD,
        "카드",
        0);
  }

  @Test
  @DisplayName("결제 목록은 mock 필드명(orderId·member·provider)과 소문자 status, 페이지·합계를 내려준다")
  void getPayments_형태() throws Exception {
    given(
            adminPaymentService.getPayments(
                eq("paid"), eq("jin"), eq(LocalDate.of(2026, 11, 1)), isNull(), eq(0), eq(50)))
        .willReturn(
            new AdminPaymentPageResponse(
                List.of(item(UUID.randomUUID())),
                0,
                50,
                1,
                new AdminPaymentPageResponse.Summary(
                    1, 3000, 0, 0, Map.of(PaymentCurrency.KRW, new BigDecimal("3000")), Map.of())));

    mockMvc
        .perform(
            get("/api/v1/admin/payments")
                .param("status", "paid")
                .param("q", "jin")
                .param("from", "2026-11-01")
                .header("X-Admin-Key", ADMIN_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].orderId").value("DIA-20261104-ABCDE12345"))
        .andExpect(jsonPath("$.items[0].member.email").value("jin@example.com"))
        .andExpect(jsonPath("$.items[0].provider").value("toss"))
        .andExpect(jsonPath("$.items[0].status").value("paid"))
        .andExpect(jsonPath("$.items[0].method").value("CARD"))
        .andExpect(jsonPath("$.items[0].methodDetail").value("카드"))
        .andExpect(jsonPath("$.items[0].currency").value("KRW"))
        .andExpect(jsonPath("$.items[0].amount").value(3000))
        .andExpect(jsonPath("$.items[0].diary.deleted").value(false))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.summary.paidAmount").value(3000))
        .andExpect(jsonPath("$.summary.paidAmounts.KRW").value(3000));
  }

  @Test
  @DisplayName("상세는 목록 항목을 펼친 형태에 paymentKey·refundRequest 를 더한다")
  void getPayment_펼친형태() throws Exception {
    UUID id = UUID.randomUUID();
    UUID inquiryId = UUID.randomUUID();
    given(adminPaymentService.getPayment(id))
        .willReturn(
            new AdminPaymentDetailResponse(
                item(id),
                "pk_1",
                null,
                null,
                null,
                new AdminPaymentDetailResponse.RefundRequest(inquiryId, "requested")));

    mockMvc
        .perform(get("/api/v1/admin/payments/{id}", id).header("X-Admin-Key", ADMIN_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.orderId").value("DIA-20261104-ABCDE12345"))
        .andExpect(jsonPath("$.paymentKey").value("pk_1"))
        .andExpect(jsonPath("$.payment").doesNotExist())
        .andExpect(jsonPath("$.refundRequest.inquiryId").value(inquiryId.toString()))
        .andExpect(jsonPath("$.refundRequest.status").value("requested"));
  }

  @Test
  @DisplayName("없는 결제는 404 PAYMENT_NOT_FOUND, 날짜 형식 오류는 400, 어드민 키 없으면 403")
  void 오류응답() throws Exception {
    UUID id = UUID.randomUUID();
    given(adminPaymentService.getPayment(id))
        .willThrow(new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

    mockMvc
        .perform(get("/api/v1/admin/payments/{id}", id).header("X-Admin-Key", ADMIN_KEY))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
    mockMvc
        .perform(
            get("/api/v1/admin/payments").param("from", "11/01").header("X-Admin-Key", ADMIN_KEY))
        .andExpect(status().isBadRequest());
    mockMvc.perform(get("/api/v1/admin/payments")).andExpect(status().isForbidden());
    verify(adminPaymentService, never())
        .getPayments(any(), any(), any(), any(), anyInt(), anyInt());
  }
}
