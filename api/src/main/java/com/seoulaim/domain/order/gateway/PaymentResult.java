package com.seoulaim.domain.order.gateway;

import java.time.Instant;

public sealed interface PaymentResult {

  String rawBody();

  static PaymentResult success(
      String paymentKey, String method, Instant approvedAt, String rawBody) {
    return success(paymentKey, method, null, approvedAt, rawBody);
  }

  static PaymentResult success(
      String paymentKey,
      String method,
      String easyPayProvider,
      Instant approvedAt,
      String rawBody) {
    return new Success(paymentKey, method, easyPayProvider, approvedAt, rawBody);
  }

  static PaymentResult failure(String code, String message, String rawBody) {
    return new Failure(code, message, rawBody);
  }

  /**
   * @param method 토스 응답 method (카드, 간편결제, 해외간편결제 등). 없으면 null
   * @param easyPayProvider 토스 응답 easyPay.provider (토스페이, PAYPAL 등). 간편결제가 아니면 null
   */
  record Success(
      String paymentKey, String method, String easyPayProvider, Instant approvedAt, String rawBody)
      implements PaymentResult {

    /** 저장·표시용 결제수단 상세: "카드", "간편결제(토스페이)". 둘 다 없으면 null. */
    public String methodDetail() {
      if (easyPayProvider == null || easyPayProvider.isBlank()) {
        return method;
      }
      return method == null ? easyPayProvider : method + "(" + easyPayProvider + ")";
    }
  }

  record Failure(String code, String message, String rawBody) implements PaymentResult {}
}
