package com.seoulaim.domain.diary.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seoulaim.domain.diary.dto.DiaryCheckoutResponse;
import com.seoulaim.domain.diary.dto.DiaryPaymentConfirmResponse;
import com.seoulaim.domain.diary.dto.DiaryPaymentCreateResponse;
import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.service.DiaryPaymentService;
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

@WebMvcTest(DiaryPaymentController.class)
@Import(SecurityTestConfig.class)
class DiaryPaymentControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private TestAccessTokens tokens;
  @MockitoBean private DiaryPaymentService diaryPaymentService;

  @Test
  @DisplayName("결제 화면은 item, 빈 coupons, TOSS 결제수단을 반환한다")
  void getCheckout_정상_200반환() throws Exception {
    UUID userId = UUID.randomUUID();
    Diary diary = Diary.create(userId, false);
    given(diaryPaymentService.getCheckout(userId, diary.getId()))
        .willReturn(DiaryCheckoutResponse.of(diary, 3000L));

    mockMvc
        .perform(
            get("/api/v1/diaries/{diaryId}/checkout", diary.getId())
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(userId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.item.diaryId").value(diary.getId().toString()))
        .andExpect(jsonPath("$.item.price").value(3000))
        .andExpect(jsonPath("$.coupons").isEmpty())
        .andExpect(jsonPath("$.paymentMethods[0].id").value("TOSS"))
        .andExpect(jsonPath("$.paymentMethods[0].name").value("Card"));
  }

  @Test
  @DisplayName("결제 생성은 201 과 orderId·orderNumber·tossClientKey 를 반환한다")
  void createPayment_정상_201반환() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID diaryId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    given(diaryPaymentService.createPayment(eq(userId), eq(diaryId), any()))
        .willReturn(
            new DiaryPaymentCreateResponse(
                orderId, "DIA-20261103-ABCDE12345", 3000L, "Seoulaim Diary", "test_ck"));

    mockMvc
        .perform(
            post("/api/v1/diaries/{diaryId}/payments", diaryId)
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"couponId\": null, \"refundConsent\": true}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orderId").value(orderId.toString()))
        .andExpect(jsonPath("$.orderNumber").value("DIA-20261103-ABCDE12345"))
        .andExpect(jsonPath("$.finalAmount").value(3000))
        .andExpect(jsonPath("$.orderName").value("Seoulaim Diary"));
  }

  @Test
  @DisplayName("승인 성공 시 200 과 diaryId 를 반환한다")
  void confirm_성공_200반환() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    UUID diaryId = UUID.randomUUID();
    given(diaryPaymentService.confirm(eq(userId), eq(orderId), any()))
        .willReturn(new DiaryPaymentConfirmResponse(diaryId));

    mockMvc
        .perform(
            post("/api/v1/payments/{orderId}/confirm", orderId)
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentKey\": \"pk_1\", \"amount\": 3000}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.diaryId").value(diaryId.toString()));
  }

  @Test
  @DisplayName("토스 승인 실패면 402 PAYMENT_FAILED 와 fields.code 를 반환한다")
  void confirm_토스실패_402반환() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    given(diaryPaymentService.confirm(eq(userId), eq(orderId), any()))
        .willThrow(
            new BusinessException(
                ErrorCode.PAYMENT_FAILED,
                Map.of("code", "REJECT_CARD_COMPANY", "message", "카드사 거절")));

    mockMvc
        .perform(
            post("/api/v1/payments/{orderId}/confirm", orderId)
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentKey\": \"pk_1\", \"amount\": 3000}"))
        .andExpect(status().isPaymentRequired())
        .andExpect(jsonPath("$.code").value("PAYMENT_FAILED"))
        .andExpect(jsonPath("$.fields.code").value("REJECT_CARD_COMPANY"));
  }

  @Test
  @DisplayName("access token 없이 결제 내역을 조회하면 401 을 반환한다")
  void getMyPayments_토큰없음_401반환() throws Exception {
    mockMvc.perform(get("/api/v1/me/payments")).andExpect(status().isUnauthorized());

    verify(diaryPaymentService, never()).getMyPayments(any());
  }

  @Test
  @DisplayName("edited 는 204 를 반환한다")
  void markEdited_정상_204반환() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID diaryId = UUID.randomUUID();

    mockMvc
        .perform(
            post("/api/v1/diaries/{diaryId}/edited", diaryId)
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(userId)))
        .andExpect(status().isNoContent());

    verify(diaryPaymentService).markEdited(userId, diaryId);
  }
}
