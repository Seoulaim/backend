package com.seoulaim.domain.order.gateway;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 결제 통화. 금액은 통화 단위 숫자(원, 달러)로 다루고 소수 자릿수는 통화별로 고정한다. */
public enum PaymentCurrency {
  KRW(0),
  USD(2);

  private final int fractionDigits;

  PaymentCurrency(int fractionDigits) {
    this.fractionDigits = fractionDigits;
  }

  public int fractionDigits() {
    return fractionDigits;
  }

  /**
   * 통화 소수 자릿수로 맞춘다 (KRW 3000.00 → 3000, USD 2.9 → 2.90). 자릿수를 넘는 값은 예외.
   *
   * @throws ArithmeticException 소수 자릿수를 넘는 값 (예: KRW 3000.5, USD 2.999)
   */
  public BigDecimal normalize(BigDecimal amount) {
    return amount.setScale(fractionDigits, RoundingMode.UNNECESSARY);
  }

  /** 소수 자릿수 안의 값이고 normalize 결과가 같으면 같은 금액. 2.99 와 2.990 은 같고 2.999 는 어느 값과도 다르다. */
  public boolean sameAmount(BigDecimal expected, BigDecimal actual) {
    if (expected == null
        || actual == null
        || actual.stripTrailingZeros().scale() > fractionDigits) {
      return false;
    }
    return normalize(expected).compareTo(normalize(actual)) == 0;
  }
}
