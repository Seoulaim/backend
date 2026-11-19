package com.seoulaim.domain.auth.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 구글 로그인 설정 (app.auth.google)
 *
 * <ul>
 *   <li>clientIds: 허용할 OAuth 클라이언트 ID (웹·iOS·Android). ID 토큰 aud 와 비교
 *   <li>환경변수 APP_AUTH_GOOGLE_CLIENT_IDS 에 쉼표로 구분해 설정
 *   <li>비어 있으면 구글 로그인 비활성 (AUTH_SOCIAL_UNAVAILABLE)
 * </ul>
 */
@ConfigurationProperties("app.auth.google")
public record GoogleOAuthProperties(@DefaultValue List<String> clientIds) {}
