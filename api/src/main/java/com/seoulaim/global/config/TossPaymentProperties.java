package com.seoulaim.global.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

/**
 * 토스페이먼츠 키. secretKey 는 서버 승인 API 에, clientKey 는 결제창 SDK 에 쓴다.
 *
 * <p>토스는 상점(MID) 하나가 통화 하나만 받는다. 최상위 키는 원화 MID(카드), {@link #foreign} 은 외화 MID(PayPal, USD).
 *
 * @param foreign 외화 MID. 설정이 없으면 키가 빈 값인 {@link Foreign}
 */
@Validated
@ConfigurationProperties(prefix = "app.payment.toss")
public record TossPaymentProperties(@NotBlank String secretKey, String clientKey, Foreign foreign) {

  @ConstructorBinding
  public TossPaymentProperties {
    if (foreign == null) {
      foreign = new Foreign(null, null, null);
    }
  }

  /** 원화 MID 만 쓰는 경우 (외화 MID 미설정). */
  public TossPaymentProperties(String secretKey, String clientKey) {
    this(secretKey, clientKey, null);
  }

  /**
   * 외화 MID (결제위젯 v2 키 {@code *_gck_} / {@code *_gsk_}). 키가 하나라도 비어 있으면 PayPal 을 쓰지 않는다.
   *
   * @param paypalVariantKey 결제위젯 UI variantKey. 비어 있으면 "PAYPAL"
   */
  public record Foreign(String clientKey, String secretKey, String paypalVariantKey) {

    public static final String DEFAULT_PAYPAL_VARIANT_KEY = "PAYPAL";

    public Foreign {
      if (paypalVariantKey == null || paypalVariantKey.isBlank()) {
        paypalVariantKey = DEFAULT_PAYPAL_VARIANT_KEY;
      }
    }

    /** 클라이언트 키·시크릿 키가 모두 있으면 PayPal 결제를 받을 수 있다. */
    public boolean available() {
      return clientKey != null && !clientKey.isBlank() && secretKey != null && !secretKey.isBlank();
    }
  }
}
