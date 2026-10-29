package com.seoulaim.domain.order.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulaim.global.config.TossPaymentProperties;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.io.IOException;
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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** 토스페이먼츠 결제 승인 API 구현. https://docs.tosspayments.com/reference#결제-승인 */
@Slf4j
@Component
public class TossPaymentGateway implements PaymentGateway {

  static final String BASE_URL = "https://api.tosspayments.com";
  static final String CONFIRM_PATH = "/v1/payments/confirm";
  private static final String UNKNOWN_CODE = "UNKNOWN";

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public TossPaymentGateway(
      RestClient.Builder restClientBuilder,
      TossPaymentProperties properties,
      ObjectMapper objectMapper) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(5));
    // 토스 승인 API 는 카드사 응답을 기다리므로 읽기 타임아웃을 넉넉히 둔다
    requestFactory.setReadTimeout(Duration.ofSeconds(30));
    this.restClient =
        restClientBuilder
            .baseUrl(BASE_URL)
            .requestFactory(requestFactory)
            .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth(properties.secretKey()))
            .build();
    this.objectMapper = objectMapper;
  }

  @Override
  public PaymentResult confirm(String paymentKey, String orderId, long amount) {
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
