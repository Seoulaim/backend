package com.seoulaim.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.auth.repository.RefreshTokenRedisRepository;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.entity.UserStatus;
import com.seoulaim.domain.user.service.UserService;
import com.seoulaim.global.config.JwtProperties;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.security.JwtTokenProvider;
import com.seoulaim.global.security.RefreshTokenClaims;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  private static final String EMAIL = "luna@example.com";

  @Mock private UserService userService;
  @Mock private EmailVerificationService emailVerificationService;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtTokenProvider jwtTokenProvider;
  @Mock private JwtProperties jwtProperties;
  @Mock private RefreshTokenRedisRepository refreshTokenRedisRepository;

  @InjectMocks private AuthService authService;

  @Test
  @DisplayName("비밀번호가 틀리면 AUTH_INVALID_CREDENTIALS 예외가 발생한다")
  void login_비밀번호불일치_예외() {
    User user = User.createWithEmail(EMAIL, "hash", "Luna");
    given(userService.findByEmail(EMAIL)).willReturn(Optional.of(user));
    given(passwordEncoder.matches("wrong-pass1", "hash")).willReturn(false);

    assertThatThrownBy(() -> authService.login("Luna@Example.com", "wrong-pass1"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
  }

  @Test
  @DisplayName("가입되지 않은 이메일이면 비밀번호 불일치와 같은 AUTH_INVALID_CREDENTIALS 예외가 발생한다")
  void login_없는이메일_예외() {
    given(userService.findByEmail(EMAIL)).willReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(EMAIL, "seoulaim1234"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
  }

  @Test
  @DisplayName("정지된 계정이면 비밀번호가 맞아도 USER_SUSPENDED 예외가 발생한다")
  void login_정지계정_예외() {
    User user = User.createWithEmail(EMAIL, "hash", "Luna");
    ReflectionTestUtils.setField(user, "status", UserStatus.SUSPENDED);
    given(userService.findByEmail(EMAIL)).willReturn(Optional.of(user));
    given(passwordEncoder.matches("seoulaim1234", "hash")).willReturn(true);

    assertThatThrownBy(() -> authService.login(EMAIL, "seoulaim1234"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_SUSPENDED);
  }

  @Test
  @DisplayName("이미 사용한 refresh token 으로 재발급하면 AUTH_INVALID_TOKEN 예외가 발생한다")
  void refresh_폐기된토큰_예외() {
    UUID userId = UUID.randomUUID();
    given(jwtTokenProvider.parseRefreshToken("used"))
        .willReturn(new RefreshTokenClaims(userId, "jti"));
    given(refreshTokenRedisRepository.delete(userId, "jti")).willReturn(false);

    assertThatThrownBy(() -> authService.refresh("used"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_INVALID_TOKEN);
    verify(jwtTokenProvider, never()).issueAccessToken(any());
  }

  @Test
  @DisplayName("다른 유저의 refresh token 으로 로그아웃하면 삭제하지 않고 성공 처리한다")
  void logout_다른유저토큰_삭제안함() {
    UUID otherUserId = UUID.randomUUID();
    given(jwtTokenProvider.parseRefreshToken("token"))
        .willReturn(new RefreshTokenClaims(otherUserId, "jti"));

    authService.logout(UUID.randomUUID(), "token");

    verify(refreshTokenRedisRepository, never()).delete(any(), anyString());
  }

  @Test
  @DisplayName("만료·위조된 refresh token 으로 로그아웃해도 예외 없이 성공 처리한다")
  void logout_잘못된토큰_성공처리() {
    given(jwtTokenProvider.parseRefreshToken("expired"))
        .willThrow(new BusinessException(ErrorCode.AUTH_EXPIRED_TOKEN));

    assertThatCode(() -> authService.logout(UUID.randomUUID(), "expired"))
        .doesNotThrowAnyException();
  }
}
