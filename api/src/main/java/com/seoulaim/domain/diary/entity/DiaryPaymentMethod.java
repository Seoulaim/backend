package com.seoulaim.domain.diary.entity;

import com.seoulaim.domain.order.gateway.PaymentCurrency;
import com.seoulaim.domain.order.gateway.PaymentMerchant;

/** 회원이 고른 결제수단. 수단마다 토스 상점(MID)과 통화가 정해진다. */
public enum DiaryPaymentMethod {
  /** 원화 카드. 원화 MID 결제창(v1) */
  CARD(PaymentMerchant.KRW, PaymentCurrency.KRW),
  /** PayPal. 외화 MID 결제위젯(v2), USD */
  PAYPAL(PaymentMerchant.FOREIGN, PaymentCurrency.USD);

  private final PaymentMerchant merchant;
  private final PaymentCurrency currency;

  DiaryPaymentMethod(PaymentMerchant merchant, PaymentCurrency currency) {
    this.merchant = merchant;
    this.currency = currency;
  }

  public PaymentMerchant merchant() {
    return merchant;
  }

  public PaymentCurrency currency() {
    return currency;
  }
}
