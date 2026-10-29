package com.seoulaim.domain.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seoulaim.domain.order.dto.CreateOrderResponse;
import com.seoulaim.domain.order.service.OrderService;
import com.seoulaim.domain.order.service.PaymentService;
import com.seoulaim.global.security.SecurityTestConfig;
import com.seoulaim.global.security.TestAccessTokens;
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

@WebMvcTest(OrderController.class)
@Import(SecurityTestConfig.class)
class OrderControllerTest {

  private static final String REQUEST_BODY =
      """
      {
        "items": [{"productId": "0199a1b2-0000-7000-8000-000000000001", "quantity": 1}],
        "recipientName": "Kim",
        "addressLine1": "서울특별시 중구 세종대로 110",
        "addressLine2": "101호",
        "postalCode": "04524",
        "contactType": "KAKAO",
        "contactValue": "seoul_fan"
      }
      """;

  @Autowired private MockMvc mockMvc;
  @Autowired private TestAccessTokens tokens;
  @MockitoBean private OrderService orderService;
  @MockitoBean private PaymentService paymentService;

  @Test
  @DisplayName("올바른 주문 요청이면 201 과 주문번호·결제 금액·토스 클라이언트 키를 반환한다")
  void createOrder_정상요청_201반환() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    given(orderService.create(eq(userId), any()))
        .willReturn(new CreateOrderResponse(orderId, "ORD-20261029-AB12CD", 28000L, "test_ck"));

    mockMvc
        .perform(
            post("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_BODY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orderId").value(orderId.toString()))
        .andExpect(jsonPath("$.orderNumber").value("ORD-20261029-AB12CD"))
        .andExpect(jsonPath("$.finalAmount").value(28000))
        .andExpect(jsonPath("$.tossClientKey").value("test_ck"));
  }

  @Test
  @DisplayName("access token 없이 주문하면 401 을 반환한다")
  void createOrder_헤더없음_401반환() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(REQUEST_BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_INVALID_TOKEN"));

    verify(orderService, never()).create(any(), any());
  }

  @Test
  @DisplayName("상품 목록이 비어 있고 연락 방식이 잘못되면 400 과 필드 오류를 반환한다")
  void createOrder_검증실패_400반환() throws Exception {
    String body =
        """
        {"items": [], "recipientName": "Kim", "addressLine1": "서울 중구 1", "contactType": "EMAIL"}
        """;

    mockMvc
        .perform(
            post("/api/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(UUID.randomUUID()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_INVALID_REQUEST"))
        .andExpect(jsonPath("$.fields.items").exists())
        .andExpect(jsonPath("$.fields.contactType").exists());

    verify(orderService, never()).create(any(), any());
  }
}
