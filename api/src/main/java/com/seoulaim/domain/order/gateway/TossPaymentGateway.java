package com.seoulaim.domain.order.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 토스페이먼츠 결제 승인·취소 API 구현.
 *
 * <ul>
 *   <li>승인: https://docs.tosspayments.com/reference#결제-승인
 *   <li>취소: https://docs.tosspayments.com/reference#결제-취소
 * </ul>
 *
 * <p>시크릿 키(상점 MID)마다 인스턴스를 하나씩 만든다. 빈 등록은 {@link TossPaymentGatewayConfig}.
 */
@Slf4j
public class TossPaymentGateway implements PaymentGateway {

  static final String BASE_URL = "https://api.tosspayments.com";
  static final String CONFIRM_PATH = "/v1/payments/confirm";
  static final String CANCEL_PATH = "/v1/payments/{paymentKey}/cancel";
  static final String GATEWAY_ERROR_CODE = "GATEWAY_ERROR";
  private static final String UNKNOWN_CODE = "UNKNOWN";

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public TossPaymentGateway(
      RestClient.Builder restClientBuilder, String secretKey, ObjectMapper objectMapper) {
    this(buildRestClient(restClientBuilder, secretKey), objectMapper);
  }

  // 테스트에서 MockRestServiceServer 에 묶인 RestClient 를 넣는다
  TossPaymentGateway(RestClient restClient, ObjectMapper objectMapper) {
    this.restClient = restClient;
    this.objectMapper = objectMapper;
  }

  private static RestClient buildRestClient(
      RestClient.Builder restClientBuilder, String secretKey) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(5));
    // 토스 승인·취소 API 는 카드사 응답을 기다리므로 읽기 타임아웃을 넉넉히 둔다
    requestFactory.setReadTimeout(Duration.ofSeconds(30));
    // 상점마다 기본 Authorization 헤더가 달라 빌더를 복제한다 (같은 빌더에 쌓이지 않게)
    return restClientBuilder
        .clone()
        .baseUrl(BASE_URL)
        .requestFactory(requestFactory)
        .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth(secretKey))
        .build();
  }

  @Override
  public PaymentResult confirm(String paymentKey, String orderId, BigDecimal amount) {
    try {
      return restClient
          .post()
          .uri(CONFIRM_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .body(Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount))
          .exchange(
              (request, response) -> {
                String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                return toResult(response.getStatusCode(), body);
              });
    } catch (RestClientException exception) {
      log.error("토스 결제 승인 통신 실패 orderId={}", orderId, exception);
      throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_ERROR);
    }
  }

  @Override
  public PaymentCancelResult cancel(String paymentKey, String cancelReason) {
    try {
      return restClient
          .post()
          .uri(CANCEL_PATH, paymentKey)
          .contentType(MediaType.APPLICATION_JSON)
          .body(Map.of("cancelReason", cancelReason))
          .exchange(
              (request, response) -> {
                String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                return toCancelResult(response.getStatusCode(), body);
              });
    } catch (RestClientException exception) {
      log.error("토스 결제 취소 통신 실패 paymentKey={}", paymentKey, exception);
      throw refundGatewayError();
    }
  }

  private PaymentCancelResult toCancelResult(HttpStatusCode status, String body) {
    if (status.is2xxSuccessful()) {
      JsonNode json = readJson(body);
      if (json == null) {
        log.error("토스 결제 취소 응답 파싱 실패 status={}", status);
        throw refundGatewayError();
      }
      // 이번 취소는 cancels 배열의 마지막 항목
      JsonNode cancels = json.path("cancels");
      JsonNode latest =
          cancels.isArray() && !cancels.isEmpty() ? cancels.get(cancels.size() - 1) : null;
      return PaymentCancelResult.success(
          latest == null ? null : latest.path("transactionKey").asText(null),
          latest == null ? null : parseInstant(latest.path("canceledAt").asText(null)),
          body);
    }
    if (status.is4xxClientError()) {
      JsonNode json = readJson(body);
      if (json == null) {
        return PaymentCancelResult.failure(UNKNOWN_CODE, null, null);
      }
      return PaymentCancelResult.failure(
          json.path("code").asText(UNKNOWN_CODE), json.path("message").asText(null), body);
    }
    log.error("토스 결제 취소 서버 오류 status={} body={}", status, body);
    throw refundGatewayError();
  }

  private static BusinessException refundGatewayError() {
    return new BusinessException(
        ErrorCode.PAYMENT_REFUND_FAILED,
        Map.of("code", GATEWAY_ERROR_CODE, "message", "토스페이먼츠와 통신하지 못했습니다"));
  }

  private PaymentResult toResult(HttpStatusCode status, String body) throws IOException {
    if (status.is2xxSuccessful()) {
      JsonNode json = readJson(body);
      if (json == null) {
        log.error("토스 결제 승인 응답 파싱 실패 status={}", status);
        throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_ERROR);
      }
      return PaymentResult.success(
          json.path("paymentKey").asText(null),
          json.path("method").asText(null),
          json.path("easyPay").path("provider").asText(null),
          parseInstant(json.path("approvedAt").asText(null)),
          body);
    }
    if (status.is4xxClientError()) {
      JsonNode json = readJson(body);
      if (json == null) {
        return PaymentResult.failure(UNKNOWN_CODE, null, null);
      }
      return PaymentResult.failure(
          json.path("code").asText(UNKNOWN_CODE), json.path("message").asText(null), body);
    }
    log.error("토스 결제 승인 서버 오류 status={} body={}", status, body);
    throw new BusinessException(ErrorCode.PAYMENT_GATEWAY_ERROR);
  }

  // raw_response 는 jsonb 컬럼이므로 JSON 이 아니면 null 로 취급한다
  private JsonNode readJson(String body) {
    try {
      JsonNode json = objectMapper.readTree(body);
      return json != null && json.isObject() ? json : null;
    } catch (JsonProcessingException exception) {
      return null;
    }
  }

  private static Instant parseInstant(String value) {
    if (value == null) {
      return null;
    }
    try {
      return OffsetDateTime.parse(value).toInstant();
    } catch (DateTimeParseException exception) {
      return null;
    }
  }

  private static String basicAuth(String secretKey) {
    String credentials = secretKey + ":";
    return "Basic "
        + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }
}
