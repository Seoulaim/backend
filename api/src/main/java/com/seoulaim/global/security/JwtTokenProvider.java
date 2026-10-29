package com.seoulaim.global.security;

import com.seoulaim.global.config.JwtProperties;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * JWT 발급·검증
 *
 * <ul>
 *   <li>access·refresh 모두 HMAC-SHA 서명, subject 에 userId
 *   <li>typ 클레임으로 access·refresh 구분 → refresh token 으로 API 호출 차단
 *   <li>refresh token 의 jti(tokenId) 는 Redis 에 저장해 폐기·재사용 탐지
 * </ul>
 */
@Component
public class JwtTokenProvider {

  private static final String TOKEN_TYPE_CLAIM = "typ";
  private static final String ACCESS_TYPE = "access";
  private static final String REFRESH_TYPE = "refresh";

  private final SecretKey key;
  private final Duration accessTokenExpiration;
  private final Duration refreshTokenExpiration;
  private final Clock clock;

  public JwtTokenProvider(JwtProperties properties, Clock clock) {
    this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    this.accessTokenExpiration = properties.accessTokenExpiration();
    this.refreshTokenExpiration = properties.refreshTokenExpiration();
    this.clock = clock;
  }

  public IssuedToken issueAccessToken(UUID userId) {
    return issue(userId, ACCESS_TYPE, accessTokenExpiration);
  }

  public IssuedToken issueRefreshToken(UUID userId) {
    return issue(userId, REFRESH_TYPE, refreshTokenExpiration);
  }

  public UUID parseAccessToken(String token) {
    return UUID.fromString(parse(token, ACCESS_TYPE).getSubject());
  }

  public RefreshTokenClaims parseRefreshToken(String token) {
    Claims claims = parse(token, REFRESH_TYPE);
    return new RefreshTokenClaims(UUID.fromString(claims.getSubject()), claims.getId());
  }

  private IssuedToken issue(UUID userId, String type, Duration expiration) {
    Instant now = clock.instant();
    Instant expiresAt = now.plus(expiration);
    String tokenId = UUID.randomUUID().toString();

    String token =
        Jwts.builder()
            .id(tokenId)
            .subject(userId.toString())
            .claim(TOKEN_TYPE_CLAIM, type)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(key)
            .compact();

    return new IssuedToken(token, tokenId, expiresAt);
  }

  // 만료와 위조를 구분 → 클라이언트가 재발급할지 재로그인할지 판단
  private Claims parse(String token, String expectedType) {
    Claims claims;
    try {
      claims =
          Jwts.parser()
              .verifyWith(key)
              .clock(() -> Date.from(clock.instant()))
              .build()
              .parseSignedClaims(token)
              .getPayload();
    } catch (ExpiredJwtException e) {
      throw new BusinessException(ErrorCode.AUTH_EXPIRED_TOKEN);
    } catch (JwtException | IllegalArgumentException e) {
      throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
    }

    if (!expectedType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
      throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
    }
    return claims;
  }
}
