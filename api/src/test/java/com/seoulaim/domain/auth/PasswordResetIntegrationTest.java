package com.seoulaim.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.auth.dto.SendEmailVerificationResponse;
import com.seoulaim.domain.auth.dto.TokenResponse;
import com.seoulaim.domain.auth.repository.EmailVerificationRedisRepository;
import com.seoulaim.domain.auth.service.AuthService;
import com.seoulaim.domain.auth.service.EmailVerificationService;
import com.seoulaim.domain.user.service.UserService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.mail.EmailSender;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** 재설정 인증번호 발송 → 재설정 → 새 비밀번호 로그인을 실제 DB·Redis 로 검증한다. 메일만 mock */
@Tag("integration")
@SpringBootTest
@Testcontainers
class PasswordResetIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @Container
  @ServiceConnection(name = "redis")
  static GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  private static final String EMAIL = "luna@example.com";
  private static final String OLD_PASSWORD = "seoulaim1234";
  private static final String NEW_PASSWORD = "newpass5678";
  private static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");

  @MockitoBean private EmailSender emailSender;

  @Autowired private EmailVerificationService emailVerificationService;
  @Autowired private EmailVerificationRedisRepository emailVerificationRedisRepository;
  @Autowired private AuthService authService;
  @Autowired private UserService userService;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  @DisplayName("재설정 후 새 비밀번호로만 로그인되고 기존 refresh token 은 폐기된다. 가입 인증번호로는 재설정 불가")
  void resetPassword_재설정후_새비밀번호로그인() {
    userService.createUser(EMAIL, passwordEncoder.encode(OLD_PASSWORD), "Luna");
    TokenResponse oldTokens = authService.login(EMAIL, OLD_PASSWORD);

    // 같은 이메일의 가입 인증번호(SIGNUP 키)는 재설정에 쓸 수 없다
    emailVerificationRedisRepository.saveCode(EMAIL, "111111", Duration.ofMinutes(5));
    assertThatThrownBy(() -> authService.resetPassword(EMAIL, "111111", NEW_PASSWORD))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_OTP_EXPIRED);

    emailVerificationService.sendPasswordResetCode("Luna@Example.com");
    ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
    verify(emailSender).send(eq(EMAIL), anyString(), text.capture());
    Matcher matcher = CODE.matcher(text.getValue());
    assertThat(matcher.find()).isTrue();
    String code = matcher.group(1);

    // 규칙 위반 비밀번호는 인증번호를 소모하지 않는다
    assertThatThrownBy(() -> authService.resetPassword(EMAIL, code, "short"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_INVALID_PASSWORD);

    authService.resetPassword(EMAIL, code, NEW_PASSWORD);

    assertThatThrownBy(() -> authService.login(EMAIL, OLD_PASSWORD))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
    assertThat(authService.login(EMAIL, NEW_PASSWORD).accessToken()).isNotBlank();
    assertThatThrownBy(() -> authService.refresh(oldTokens.refreshToken()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_INVALID_TOKEN);
    // 한 번 쓴 인증번호는 재사용 불가
    assertThatThrownBy(() -> authService.resetPassword(EMAIL, code, "another1234"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_OTP_EXPIRED);
  }

  @Test
  @DisplayName("미가입 이메일도 같은 응답을 받고 메일은 발송되지 않는다")
  void sendPasswordResetCode_미가입이메일_같은응답() {
    SendEmailVerificationResponse response =
        emailVerificationService.sendPasswordResetCode("nobody@example.com");

    assertThat(response.expiresInSeconds()).isEqualTo(300);
    assertThat(response.resendAvailableInSeconds()).isEqualTo(60);
    verify(emailSender, never()).send(eq("nobody@example.com"), anyString(), anyString());
  }
}
