package com.seoulaim.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.seoulaim.global.config.JwtProperties;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

  private static final String SECRET = "test-only-secret-key-must-be-at-least-256-bits";
  private static final Duration ACCESS_EXPIRATION = Duration.ofHours(1);
  private static final Duration REFRESH_EXPIRATION = Duration.ofDays(14);
  private static final Instant NOW = Instant.parse("2026-10-29T00:00:00Z");

  private final UUID userId = UUID.randomUUID();

  private JwtTokenProvider providerAt(Instant now, String secret) {
    return new JwtTokenProvider(
        new JwtProperties(secret, ACCESS_EXPIRATION, REFRESH_EXPIRATION),
        Clock.fixed(now, ZoneOffset.UTC));
  }

  private JwtTokenProvider providerAt(Instant now) {
    return providerAt(now, SECRET);
  }

  @Test
  @DisplayName("발급한 access token 에서 userId 를 꺼낸다")
  void parseAccessToken_정상토큰_userId반환() {
    JwtTokenProvider provider = providerAt(NOW);
    String token = provider.issueAccessToken(userId).token();

    assertThat(provider.parseAccessToken(token)).isEqualTo(userId);
  }

  @Test
  @DisplayName("만료된 access token 은 AUTH_EXPIRED_TOKEN 예외가 발생한다")
  void parseAccessToken_만료토큰_만료예외() {
    String token = providerAt(NOW).issueAccessToken(userId).token();
    JwtTokenProvider later = providerAt(NOW.plus(ACCESS_EXPIRATION).plusSeconds(1));

    assertThatThrownBy(() -> later.parseAccessToken(token))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_EXPIRED_TOKEN);
  }

  @Test
  @DisplayName("refresh token 을 access token 으로 쓰면 AUTH_INVALID_TOKEN 예외가 발생한다")
  void parseAccessToken_refresh토큰_위조예외() {
    JwtTokenProvider provider = providerAt(NOW);
    String refreshToken = provider.issueRefreshToken(userId).token();

    assertThatThrownBy(() -> provider.parseAccessToken(refreshToken))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_INVALID_TOKEN);
  }

  @Test
  @DisplayName("다른 키로 서명된 토큰은 AUTH_INVALID_TOKEN 예외가 발생한다")
  void parseAccessToken_다른키서명_위조예외() {
    String token =
        providerAt(NOW, "another-secret-key-that-is-also-at-least-256-bits")
            .issueAccessToken(userId)
            .token();

    assertThatThrownBy(() -> providerAt(NOW).parseAccessToken(token))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_INVALID_TOKEN);
  }

  @Test
  @DisplayName("JWT 형식이 아닌 문자열은 AUTH_INVALID_TOKEN 예외가 발생한다")
  void parseAccessToken_형식오류_위조예외() {
    assertThatThrownBy(() -> providerAt(NOW).parseAccessToken("not-a-jwt"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_INVALID_TOKEN);
  }

  @Test
  @DisplayName("refresh token 에서 userId 와 발급 시 tokenId 를 꺼낸다")
  void parseRefreshToken_정상토큰_userId와tokenId반환() {
    JwtTokenProvider provider = providerAt(NOW);
    IssuedToken issued = provider.issueRefreshToken(userId);

    RefreshTokenClaims claims = provider.parseRefreshToken(issued.token());

    assertThat(claims.userId()).isEqualTo(userId);
    assertThat(claims.tokenId()).isEqualTo(issued.tokenId());
  }
}
