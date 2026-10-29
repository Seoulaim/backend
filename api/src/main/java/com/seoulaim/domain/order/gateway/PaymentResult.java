package com.seoulaim.domain.order.gateway;

import java.time.Instant;

public sealed interface PaymentResult {

  String rawBody();

  static PaymentResult success(
      String paymentKey, String method, Instant approvedAt, String rawBody) {
    return new Success(paymentKey, method, approvedAt, rawBody);
  }

  static PaymentResult failure(String code, String message, String rawBody) {
    return new Failure(code, message, rawBody);
  }

  record Success(String paymentKey, String method, Instant approvedAt, String rawBody)
      implements PaymentResult {}

  record Failure(String code, String message, String rawBody) implements PaymentResult {}
}
