package com.seoulaim.domain.order.gateway;

/** PG 결제 승인 추상화. 구현체는 4xx 성격의 실패를 예외 대신 {@link PaymentResult.Failure} 로 돌려준다. */
public interface PaymentGateway {

  PaymentResult confirm(String paymentKey, String orderId, long amount);
}
