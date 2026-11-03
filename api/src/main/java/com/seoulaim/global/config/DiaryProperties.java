package com.seoulaim.global.config;

import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 다이어리 가격·무료 범위.
 *
 * @param price 유료 다이어리 1권 가격 (원)
 * @param freeSpreads 첫 무료 다이어리에서 결제 없이 저장할 수 있는 최대 spread 수
 */
@Validated
@ConfigurationProperties(prefix = "app.diary")
public record DiaryProperties(@PositiveOrZero long price, @PositiveOrZero int freeSpreads) {}
