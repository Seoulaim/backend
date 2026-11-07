package com.seoulaim.domain.order.gateway;

import java.math.BigDecimal;

/**
 * PG 결제 승인·취소 추상화. 구현체는 4xx 성격의 실패를 예외 대신 결과 객체의 Failure 로 돌려준다.
 *
 * <p>토스는 상점(MID)마다 시크릿 키가 달라 MID 별 인스턴스를 둔다. 상점 선택은 {@link PaymentGatewayResolver}.
 */
public interface PaymentGateway {

  /**
   * @param amount 상점 통화 단위 금액 (KRW 3000, USD 2.99)
   */
  PaymentResult confirm(String paymentKey, String orderId, BigDecimal amount);

  /** 원화 정수 금액 승인. order 도메인(원화 주문)용. */
  default PaymentResult confirm(String paymentKey, String orderId, long amount) {
    return confirm(paymentKey, orderId, BigDecimal.valueOf(amount));
  }

  /** 전액 취소. 통신 오류·PG 서버 오류는 PAYMENT_REFUND_FAILED 예외. */
  PaymentCancelResult cancel(String paymentKey, String cancelReason);
}
