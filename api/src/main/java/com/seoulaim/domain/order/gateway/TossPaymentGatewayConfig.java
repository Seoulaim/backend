package com.seoulaim.domain.order.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulaim.global.config.TossPaymentProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 토스 상점(MID)별 게이트웨이.
 *
 * <p>{@link PaymentGateway} 빈은 원화 MID 하나뿐이다 (order 도메인이 그대로 주입받는다). 외화 MID 게이트웨이는 빈으로 두지 않고 {@link
 * PaymentGatewayResolver} 안에만 만든다.
 */
@Configuration
public class TossPaymentGatewayConfig {

  public static final String KRW_GATEWAY = "krwPaymentGateway";

  @Bean(KRW_GATEWAY)
  public PaymentGateway krwPaymentGateway(
      RestClient.Builder restClientBuilder,
      TossPaymentProperties properties,
      ObjectMapper objectMapper) {
    return new TossPaymentGateway(restClientBuilder, properties.secretKey(), objectMapper);
  }

  @Bean
  public PaymentGatewayResolver paymentGatewayResolver(
      @Qualifier(KRW_GATEWAY) PaymentGateway krwPaymentGateway,
      RestClient.Builder restClientBuilder,
      TossPaymentProperties properties,
      ObjectMapper objectMapper) {
    TossPaymentProperties.Foreign foreign = properties.foreign();
    return new PaymentGatewayResolver(
        krwPaymentGateway,
        foreign.available()
            ? new TossPaymentGateway(restClientBuilder, foreign.secretKey(), objectMapper)
            : null);
  }
}
