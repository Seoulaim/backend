package com.seoulaim.domain.order.gateway;

/**
 * 토스페이먼츠 상점(MID). MID 하나는 통화 하나만 받으므로 원화와 외화를 나눠 운영한다.
 *
 * <ul>
 *   <li>KRW: 원화 MID. 카드 결제창(v1). 키 TOSS_CLIENT_KEY / TOSS_SECRET_KEY
 *   <li>FOREIGN: 외화 MID. PayPal(USD) 결제위젯(v2). 키 TOSS_FOREIGN_CLIENT_KEY / TOSS_FOREIGN_SECRET_KEY
 * </ul>
 */
public enum PaymentMerchant {
  KRW,
  FOREIGN
}
