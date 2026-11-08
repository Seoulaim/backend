package com.seoulaim.domain.order.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.Instant;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TossPaymentGatewayTest {

  private static final String AUTH = "Basic dGVzdF9zazo=";

  private MockRestServiceServer server;
  private TossPaymentGateway gateway;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder =
        RestClient.builder()
            .baseUrl(TossPaymentGateway.BASE_URL)
            .defaultHeader(HttpHeaders.AUTHORIZATION, AUTH);
    server = MockRestServiceServer.bindTo(builder).build();
    gateway = new TossPaymentGateway(builder.build(), new ObjectMapper());
  }

  @Test
  @DisplayName("승인 성공 응답에서 paymentKey·method·approvedAt 을 읽는다")
  void confirm_성공_파싱() {
    server
        .expect(requestTo(TossPaymentGateway.BASE_URL + "/v1/payments/confirm"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                """
                {"paymentKey":"pk_1","method":"카드","approvedAt":"2026-11-05T12:00:00+09:00"}
                """,
                MediaType.APPLICATION_JSON));

    PaymentResult result = gateway.confirm("pk_1", "DIA-1", 3000L);

    assertThat(result).isInstanceOf(PaymentResult.Success.class);
    PaymentResult.Success success = (PaymentResult.Success) result;
    assertThat(success.paymentKey()).isEqualTo("pk_1");
    assertThat(success.method()).isEqualTo("카드");
    assertThat(success.approvedAt()).isEqualTo(Instant.parse("2026-11-05T03:00:00Z"));
    server.verify();
  }

  @Test
  @DisplayName("외화 MID 승인: amount 를 소수 그대로(숫자 2.99) 보내고 easyPay.provider 를 읽어 methodDetail 로 합친다")
  void confirm_USD_소수금액과easyPay() {
    server
        .expect(requestTo(TossPaymentGateway.BASE_URL + "/v1/payments/confirm"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(
            content()
                .json("{\"paymentKey\":\"pk_pp\",\"orderId\":\"DIA-1\",\"amount\":2.99}", true))
        .andRespond(
            withSuccess(
                """
                {"paymentKey":"pk_pp","method":"해외간편결제","currency":"USD","totalAmount":2.99,
                 "easyPay":{"provider":"PAYPAL","amount":2.99},"approvedAt":"2026-11-07T12:00:00+09:00"}
                """,
                MediaType.APPLICATION_JSON));

    PaymentResult result = gateway.confirm("pk_pp", "DIA-1", new BigDecimal("2.99"));

    PaymentResult.Success success = (PaymentResult.Success) result;
    assertThat(success.method()).isEqualTo("해외간편결제");
    assertThat(success.easyPayProvider()).isEqualTo("PAYPAL");
    assertThat(success.methodDetail()).isEqualTo("해외간편결제(PAYPAL)");
    server.verify();
  }

  @Test
  @DisplayName("easyPay 가 null 이면 methodDetail 은 method 그대로, 원화 정수 금액은 소수점 없이 보낸다")
  void confirm_KRW_easyPay없음() {
    server
        .expect(requestTo(TossPaymentGateway.BASE_URL + "/v1/payments/confirm"))
        .andExpect(
            content()
                .string(
                    Matchers.allOf(
                        Matchers.containsString("\"amount\":3000"),
                        Matchers.not(Matchers.containsString("3000.")))))
        .andRespond(
            withSuccess(
                """
                {"paymentKey":"pk_1","method":"카드","easyPay":null}
                """,
                MediaType.APPLICATION_JSON));

    PaymentResult.Success success =
        (PaymentResult.Success) gateway.confirm("pk_1", "DIA-1", new BigDecimal("3000"));

    assertThat(success.easyPayProvider()).isNull();
    assertThat(success.methodDetail()).isEqualTo("카드");
    server.verify();
  }

  @Test
  @DisplayName("취소는 /v1/payments/{paymentKey}/cancel 에 Basic 인증·cancelReason 을 보내고 마지막 취소 거래를 읽는다")
  void cancel_성공_마지막취소거래() {
    server
        .expect(requestTo(TossPaymentGateway.BASE_URL + "/v1/payments/pk_1/cancel"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(HttpHeaders.AUTHORIZATION, AUTH))
        .andExpect(content().json("{\"cancelReason\":\"고객 요청 환불\"}"))
        .andRespond(
            withSuccess(
                """
                {"paymentKey":"pk_1","status":"CANCELED","cancels":[
                  {"transactionKey":"tx_old","canceledAt":"2026-11-04T10:00:00+09:00"},
                  {"transactionKey":"tx_new","canceledAt":"2026-11-05T12:00:00+09:00"}]}
                """,
                MediaType.APPLICATION_JSON));

    PaymentCancelResult result = gateway.cancel("pk_1", "고객 요청 환불");

    assertThat(result).isInstanceOf(PaymentCancelResult.Success.class);
    PaymentCancelResult.Success success = (PaymentCancelResult.Success) result;
    assertThat(success.cancelKey()).isEqualTo("tx_new");
    assertThat(success.canceledAt()).isEqualTo(Instant.parse("2026-11-05T03:00:00Z"));
    server.verify();
  }

  @Test
  @DisplayName("취소 4xx 는 예외 대신 토스 code·message 를 담은 Failure")
  void cancel_4xx_Failure() {
    server
        .expect(requestTo(TossPaymentGateway.BASE_URL + "/v1/payments/pk_1/cancel"))
        .andRespond(
            withStatus(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    """
                    {"code":"NOT_CANCELABLE_PAYMENT","message":"취소 할 수 없는 결제 입니다."}
                    """));

    PaymentCancelResult result = gateway.cancel("pk_1", "고객 요청 환불");

    assertThat(result).isInstanceOf(PaymentCancelResult.Failure.class);
    PaymentCancelResult.Failure failure = (PaymentCancelResult.Failure) result;
    assertThat(failure.code()).isEqualTo("NOT_CANCELABLE_PAYMENT");
    assertThat(failure.message()).isEqualTo("취소 할 수 없는 결제 입니다.");
  }

  @Test
  @DisplayName("취소 5xx 는 502 PAYMENT_REFUND_FAILED (fields.code=GATEWAY_ERROR)")
  void cancel_5xx_환불실패예외() {
    server
        .expect(requestTo(TossPaymentGateway.BASE_URL + "/v1/payments/pk_1/cancel"))
        .andRespond(withServerError());

    assertThatThrownBy(() -> gateway.cancel("pk_1", "고객 요청 환불"))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_REFUND_FAILED);
              assertThat(exception.getFields()).containsEntry("code", "GATEWAY_ERROR");
            });
  }
}
