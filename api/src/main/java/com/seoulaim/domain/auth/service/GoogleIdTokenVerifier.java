package com.seoulaim.domain.auth.service;

import com.seoulaim.domain.auth.config.GoogleOAuthProperties;
import com.seoulaim.domain.user.entity.SocialProvider;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * 구글 ID 토큰 검증
 *
 * <ul>
 *   <li>서명: 구글 공개키(JWK), 첫 검증 시 조회 후 캐시
 *   <li>iss: accounts.google.com, aud: 설정한 클라이언트 ID 중 하나, exp·nbf
 *   <li>email 은 email_verified=true 일 때만 사용
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class GoogleIdTokenVerifier {

  private static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
  private static final Set<String> ISSUERS =
      Set.of("https://accounts.google.com", "accounts.google.com");
  private static final OAuth2Error INVALID_TOKEN =
      new OAuth2Error("invalid_token", "issuer 또는 audience 불일치", null);

  private final GoogleOAuthProperties properties;
  private volatile JwtDecoder decoder;

  public SocialProfile verify(String idToken) {
    if (properties.clientIds().isEmpty()) {
      throw new BusinessException(ErrorCode.AUTH_SOCIAL_UNAVAILABLE);
    }
    Jwt jwt;
    try {
      jwt = decoder().decode(idToken);
    } catch (JwtException exception) {
      throw new BusinessException(ErrorCode.AUTH_INVALID_SOCIAL_TOKEN);
    }
    boolean emailVerified = Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"));
    return new SocialProfile(
        SocialProvider.GOOGLE,
        jwt.getSubject(),
        emailVerified ? jwt.getClaimAsString("email") : null,
        jwt.getClaimAsString("name"));
  }

  // 첫 호출 시 생성 → 앱 시작 시 구글 네트워크 호출 없음
  private JwtDecoder decoder() {
    JwtDecoder current = decoder;
    if (current == null) {
      synchronized (this) {
        current = decoder;
        if (current == null) {
          NimbusJwtDecoder created = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build();
          created.setJwtValidator(
              new DelegatingOAuth2TokenValidator<>(
                  new JwtTimestampValidator(), issuerValidator(), audienceValidator()));
          decoder = current = created;
        }
      }
    }
    return current;
  }

  private OAuth2TokenValidator<Jwt> issuerValidator() {
    return jwt ->
        ISSUERS.contains(jwt.getClaimAsString("iss"))
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
  }

  private OAuth2TokenValidator<Jwt> audienceValidator() {
    return jwt ->
        jwt.getAudience() != null
                && jwt.getAudience().stream().anyMatch(properties.clientIds()::contains)
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
  }
}
