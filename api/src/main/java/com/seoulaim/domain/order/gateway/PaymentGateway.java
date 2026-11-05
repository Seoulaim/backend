package com.seoulaim.domain.order.gateway;

/** PG 결제 승인·취소 추상화. 구현체는 4xx 성격의 실패를 예외 대신 결과 객체의 Failure 로 돌려준다. */
public interface PaymentGateway {

  PaymentResult confirm(String paymentKey, String orderId, long amount);

  /** 전액 취소. 통신 오류·PG 서버 오류는 PAYMENT_REFUND_FAILED 예외. */
  PaymentCancelResult cancel(String paymentKey, String cancelReason);
}
