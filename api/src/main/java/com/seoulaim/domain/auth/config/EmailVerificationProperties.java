package com.seoulaim.domain.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 이메일 인증 설정 (app.auth.email-verification)
 *
 * <ul>
 *   <li>codeTtl: 인증번호 유효시간
 *   <li>resendCooldown: 같은 이메일 재발송 간격
 *   <li>maxAttempts: 인증번호 최대 시도 횟수
 *   <li>verificationTokenTtl: 회원가입용 토큰 유효시간
 * </ul>
 */
@ConfigurationProperties("app.auth.email-verification")
public record EmailVerificationProperties(
    @DefaultValue("5m") Duration codeTtl,
    @DefaultValue("60s") Duration resendCooldown,
    @DefaultValue("5") int maxAttempts,
    @DefaultValue("30m") Duration verificationTokenTtl) {}
