package com.seoulaim.domain.auth.service;

import com.seoulaim.domain.auth.dto.SignupRequest;
import com.seoulaim.domain.auth.dto.TokenResponse;
import com.seoulaim.domain.auth.repository.RefreshTokenRedisRepository;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.service.UserService;
import com.seoulaim.global.config.JwtProperties;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.security.IssuedToken;
import com.seoulaim.global.security.JwtTokenProvider;
import com.seoulaim.global.security.RefreshTokenClaims;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입·로그인·토큰 재발급·로그아웃
 *
 * <ul>
 *   <li>access token 은 무상태, refresh token 은 Redis 로 폐기 관리
 *   <li>재발급 시 refresh token 도 교체 (기존 토큰 재사용 불가)
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

  private final UserService userService;
  private final EmailVerificationService emailVerificationService;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtProperties jwtProperties;
  private final RefreshTokenRedisRepository refreshTokenRedisRepository;

  @Transactional
  public TokenResponse signup(SignupRequest request) {
    String email = EmailNormalizer.normalize(request.email());
    emailVerificationService.consumeVerificationToken(request.verificationToken(), email);
    User user =
        userService.createUser(
            email, passwordEncoder.encode(request.password()), request.nickname().trim());
    return issueTokens(user.getId());
  }

  // 이메일 없음·비밀번호 불일치 구분 없이 AUTH_INVALID_CREDENTIALS
  public TokenResponse login(String rawEmail, String password) {
    String email = EmailNormalizer.normalize(rawEmail);
    User user =
        userService
            .findByEmail(email)
            .filter(found -> passwordEncoder.matches(password, found.getPasswordHash()))
            .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));
    if (!user.canLogin()) {
      throw new BusinessException(ErrorCode.USER_SUSPENDED);
    }
    return issueTokens(user.getId());
  }

  public TokenResponse refresh(String refreshToken) {
    RefreshTokenClaims claims = jwtTokenProvider.parseRefreshToken(refreshToken);
    if (!refreshTokenRedisRepository.delete(claims.userId(), claims.tokenId())) {
      throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
    }
    if (!userService.getUser(claims.userId()).canLogin()) {
      throw new BusinessException(ErrorCode.USER_SUSPENDED);
    }
    return issueTokens(claims.userId());
  }

  // 만료·위조 토큰은 이미 사용 불가 → 그대로 성공 처리. 다른 유저의 토큰은 삭제하지 않음
  public void logout(UUID userId, String refreshToken) {
    RefreshTokenClaims claims;
    try {
      claims = jwtTokenProvider.parseRefreshToken(refreshToken);
    } catch (BusinessException exception) {
      return;
    }
    if (claims.userId().equals(userId)) {
      refreshTokenRedisRepository.delete(claims.userId(), claims.tokenId());
    }
  }

  private TokenResponse issueTokens(UUID userId) {
    IssuedToken accessToken = jwtTokenProvider.issueAccessToken(userId);
    IssuedToken refreshToken = jwtTokenProvider.issueRefreshToken(userId);
    refreshTokenRedisRepository.save(
        userId, refreshToken.tokenId(), jwtProperties.refreshTokenExpiration());
    return TokenResponse.of(accessToken, refreshToken, jwtProperties);
  }
}
