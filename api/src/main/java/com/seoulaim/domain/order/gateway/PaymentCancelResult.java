package com.seoulaim.domain.order.gateway;

import java.time.Instant;

/** PG 결제 취소(환불) 결과. 4xx 성격의 거절은 예외 대신 {@link Failure} 로 돌려준다. */
public sealed interface PaymentCancelResult {

  String rawBody();

  static PaymentCancelResult success(String cancelKey, Instant canceledAt, String rawBody) {
    return new Success(cancelKey, canceledAt, rawBody);
  }

  static PaymentCancelResult failure(String code, String message, String rawBody) {
    return new Failure(code, message, rawBody);
  }

  /**
   * @param cancelKey 토스 취소 거래 키 (cancels[].transactionKey). 응답에 없으면 null
   * @param canceledAt 응답에 없으면 null
   */
  record Success(String cancelKey, Instant canceledAt, String rawBody)
      implements PaymentCancelResult {}

  record Failure(String code, String message, String rawBody) implements PaymentCancelResult {}
}
