package com.seoulaim.domain.order.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulaim.global.config.TossPaymentProperties;
import com.seoulaim.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class TossPaymentGatewayConfigTest {

  private final TossPaymentGatewayConfig config = new TossPaymentGatewayConfig();
  private final ObjectMapper objectMapper = new ObjectMapper();

  private PaymentGatewayResolver resolver(TossPaymentProperties properties) {
    RestClient.Builder builder = RestClient.builder();
    PaymentGateway krw = config.krwPaymentGateway(builder, properties, objectMapper);
    return config.paymentGatewayResolver(krw, builder, properties, objectMapper);
  }

  @Test
  @DisplayName("외화 키가 둘 다 있으면 KRW·FOREIGN 상점 게이트웨이를 따로 만든다")
  void resolver_외화키있음_두상점() {
    PaymentGatewayResolver resolver =
        resolver(
            new TossPaymentProperties(
                "test_sk",
                "test_ck",
                new TossPaymentProperties.Foreign("test_gck", "test_gsk", "PAYPAL")));

    assertThat(resolver.isAvailable(PaymentMerchant.FOREIGN)).isTrue();
    assertThat(resolver.forMerchant(PaymentMerchant.FOREIGN))
        .isNotSameAs(resolver.forMerchant(PaymentMerchant.KRW));
  }

  @Test
  @DisplayName("외화 키가 하나라도 비면 FOREIGN 은 없고 요청 시 400 PAYMENT_METHOD_UNAVAILABLE")
  void resolver_외화키없음_400() {
    for (TossPaymentProperties properties :
        new TossPaymentProperties[] {
          new TossPaymentProperties("test_sk", "test_ck"),
          new TossPaymentProperties(
              "test_sk", "test_ck", new TossPaymentProperties.Foreign("test_gck", " ", null))
        }) {
      PaymentGatewayResolver resolver = resolver(properties);

      assertThat(resolver.isAvailable(PaymentMerchant.KRW)).isTrue();
      assertThat(resolver.isAvailable(PaymentMerchant.FOREIGN)).isFalse();
      assertThatThrownBy(() -> resolver.forMerchant(PaymentMerchant.FOREIGN))
          .extracting("errorCode")
          .isEqualTo(ErrorCode.PAYMENT_METHOD_UNAVAILABLE);
    }
  }

  @Test
  @DisplayName("variantKey 가 비어 있으면 PAYPAL")
  void foreign_variantKey기본값() {
    assertThat(new TossPaymentProperties.Foreign("a", "b", "").paypalVariantKey())
        .isEqualTo("PAYPAL");
    assertThat(new TossPaymentProperties("sk", "ck").foreign().available()).isFalse();
  }
}
