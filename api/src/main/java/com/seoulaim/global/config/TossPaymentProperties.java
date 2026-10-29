package com.seoulaim.global.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 토스페이먼츠 키. secretKey 는 서버 승인 API 에, clientKey 는 iOS 결제창 SDK 에 쓴다. */
@Validated
@ConfigurationProperties(prefix = "app.payment.toss")
public record TossPaymentProperties(@NotBlank String secretKey, String clientKey) {}
