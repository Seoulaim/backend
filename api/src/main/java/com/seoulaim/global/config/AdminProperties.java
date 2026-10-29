package com.seoulaim.global.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 관리자 API 임시 인증 키. auth 의 관리자 권한이 들어오면 제거한다. */
@Validated
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(@NotBlank String key) {}
