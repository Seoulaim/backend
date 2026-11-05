package com.seoulaim.domain.inquiry.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seoulaim.domain.inquiry.dto.InquiryCreateResponse;
import com.seoulaim.domain.inquiry.service.InquiryService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
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

@WebMvcTest(InquiryController.class)
@Import(SecurityTestConfig.class)
class InquiryControllerTest {

  private static final String BODY =
      """
      {"category":"refund","title":"Refund","content":"Please","photoFileIds":[],
       "language":"en","paymentId":"%s"}
      """;

  @Autowired private MockMvc mockMvc;
  @Autowired private TestAccessTokens tokens;
  @MockitoBean private InquiryService inquiryService;

  @Test
  @DisplayName("문의 작성은 201 과 id 를 반환한다")
  void submit_정상_201() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID inquiryId = UUID.randomUUID();
    given(inquiryService.submit(eq(userId), any()))
        .willReturn(new InquiryCreateResponse(inquiryId));

    mockMvc
        .perform(
            post("/api/v1/inquiries")
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.formatted(UUID.randomUUID())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(inquiryId.toString()));
  }

  @Test
  @DisplayName("중복 환불 신청은 409 INQUIRY_REFUND_ALREADY_REQUESTED")
  void submit_중복환불_409() throws Exception {
    UUID userId = UUID.randomUUID();
    given(inquiryService.submit(eq(userId), any()))
        .willThrow(new BusinessException(ErrorCode.INQUIRY_REFUND_ALREADY_REQUESTED));

    mockMvc
        .perform(
            post("/api/v1/inquiries")
                .header(HttpHeaders.AUTHORIZATION, tokens.bearer(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.formatted(UUID.randomUUID())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INQUIRY_REFUND_ALREADY_REQUESTED"));
  }

  @Test
  @DisplayName("토큰 없이 작성하면 401")
  void submit_토큰없음_401() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/inquiries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY.formatted(UUID.randomUUID())))
        .andExpect(status().isUnauthorized());
  }
}
