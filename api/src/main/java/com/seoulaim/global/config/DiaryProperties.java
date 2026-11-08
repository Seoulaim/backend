package com.seoulaim.global.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

/**
 * 다이어리 가격·무료 범위.
 *
 * @param price 유료 다이어리 1권 가격 (원). 원화 카드 결제 금액
 * @param freeSpreads 첫 무료 다이어리에서 결제 없이 저장할 수 있는 최대 spread 수
 * @param priceUsd 유료 다이어리 1권 가격 (달러, 소수 2자리까지). PayPal 결제 금액. 없으면 2.99
 */
@Validated
@ConfigurationProperties(prefix = "app.diary")
public record DiaryProperties(
    @PositiveOrZero long price,
    @PositiveOrZero int freeSpreads,
    @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal priceUsd) {

  public static final BigDecimal DEFAULT_PRICE_USD = new BigDecimal("2.99");

  @ConstructorBinding
  public DiaryProperties {
    if (priceUsd == null) {
      priceUsd = DEFAULT_PRICE_USD;
    }
  }

  /** 달러 가격은 기본값(2.99). */
  public DiaryProperties(long price, int freeSpreads) {
    this(price, freeSpreads, null);
  }
}
