package com.seoulaim.global.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * S3 스토리지 설정. endpoint 가 비어 있으면 AWS 기본 엔드포인트를 쓴다.
 *
 * <p>accessKey/secretKey 가 비어 있으면 AWS 기본 자격증명 체인(환경변수, IAM role 등)을 쓴다.
 */
@Validated
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
    String endpoint,
    @NotBlank String region,
    @NotBlank String bucket,
    String accessKey,
    String secretKey,
    boolean pathStyleAccess) {

  public boolean hasStaticCredentials() {
    return StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey);
  }
}
