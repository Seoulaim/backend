package com.seoulaim.global.config;

import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.order")
public record OrderProperties(@PositiveOrZero long shippingFee) {}
