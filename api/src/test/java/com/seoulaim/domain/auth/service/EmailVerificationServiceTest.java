package com.seoulaim.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.auth.config.EmailVerificationProperties;
import com.seoulaim.domain.auth.dto.ConfirmEmailVerificationResponse;
import com.seoulaim.domain.auth.dto.SendEmailVerificationResponse;
import com.seoulaim.domain.auth.repository.EmailVerificationRedisRepository;
import com.seoulaim.domain.user.service.UserService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.mail.EmailSender;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

  private static final String EMAIL = "luna@example.com";

  @Mock private EmailVerificationRedisRepository repository;
  @Mock private UserService userService;
  @Mock private EmailSender emailSender;

  private EmailVerificationService service;

  @BeforeEach
  void setUp() {
    EmailVerificationProperties properties =
        new EmailVerificationProperties(
            Duration.ofMinutes(5), Duration.ofSeconds(60), 5, Duration.ofMinutes(30));
    service = new EmailVerificationService(repository, userService, emailSender, properties);
  }

  @Test
  @DisplayName("인증번호 발송 시 이메일을 소문자로 정규화해 저장·발송하고 유효시간을 반환한다")
  void sendVerificationCode_정상요청_발송() {
    given(userService.existsByEmail(EMAIL)).willReturn(false);
    given(repository.tryAcquireResendCooldown(EMAIL, Duration.ofSeconds(60))).willReturn(true);

    SendEmailVerificationResponse response = service.sendVerificationCode(" Luna@Example.com ");

    verify(repository).saveCode(eq(EMAIL), anyString(), eq(Duration.ofMinutes(5)));
    verify(emailSender).send(eq(EMAIL), anyString(), anyString());
    assertThat(response.expiresInSeconds()).isEqualTo(300);
    assertThat(response.resendAvailableInSeconds()).isEqualTo(60);
  }

  @Test
  @DisplayName("이미 가입된 이메일이면 USER_EMAIL_DUPLICATED 예외가 발생하고 발송하지 않는다")
  void sendVerificationCode_가입된이메일_예외() {
    given(userService.existsByEmail(EMAIL)).willReturn(true);

    assertThatThrownBy(() -> service.sendVerificationCode(EMAIL))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_EMAIL_DUPLICATED);
    verify(emailSender, never()).send(anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("60초 안에 재발송하면 AUTH_OTP_RESEND_TOO_SOON 예외가 발생한다")
  void sendVerificationCode_재발송제한_예외() {
    given(userService.existsByEmail(EMAIL)).willReturn(false);
    given(repository.tryAcquireResendCooldown(EMAIL, Duration.ofSeconds(60))).willReturn(false);

    assertThatThrownBy(() -> service.sendVerificationCode(EMAIL))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_OTP_RESEND_TOO_SOON);
  }

  @Test
  @DisplayName("메일 발송에 실패하면 인증번호와 재발송 제한을 지우고 AUTH_MAIL_SEND_FAILED 예외가 발생한다")
  void sendVerificationCode_발송실패_정리후예외() {
    given(userService.existsByEmail(EMAIL)).willReturn(false);
    given(repository.tryAcquireResendCooldown(EMAIL, Duration.ofSeconds(60))).willReturn(true);
    willThrow(new BusinessException(ErrorCode.AUTH_MAIL_SEND_FAILED))
        .given(emailSender)
        .send(anyString(), anyString(), anyString());

    assertThatThrownBy(() -> service.sendVerificationCode(EMAIL))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_MAIL_SEND_FAILED);
    verify(repository).deleteCode(EMAIL);
    verify(repository).deleteResendCooldown(EMAIL);
  }

  @Test
  @DisplayName("인증번호가 맞으면 인증번호를 지우고 30분짜리 verificationToken 을 발급한다")
  void confirmVerificationCode_일치_토큰발급() {
    given(repository.findCode(EMAIL)).willReturn(Optional.of("482915"));

    ConfirmEmailVerificationResponse response = service.confirmVerificationCode(EMAIL, "482915");

    verify(repository).deleteCode(EMAIL);
    verify(repository)
        .saveVerifiedEmail(eq(response.verificationToken()), eq(EMAIL), eq(Duration.ofMinutes(30)));
    assertThat(response.expiresInSeconds()).isEqualTo(1800);
  }

  @Test
  @DisplayName("발송 이력이 없거나 만료되었으면 AUTH_OTP_EXPIRED 예외가 발생한다")
  void confirmVerificationCode_인증번호없음_예외() {
    given(repository.findCode(EMAIL)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.confirmVerificationCode(EMAIL, "482915"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_OTP_EXPIRED);
  }

  @Test
  @DisplayName("인증번호가 틀리면 AUTH_OTP_MISMATCH 예외가 발생한다")
  void confirmVerificationCode_불일치_예외() {
    given(repository.findCode(EMAIL)).willReturn(Optional.of("482915"));
    given(repository.increaseAttempts(EMAIL, Duration.ofMinutes(5))).willReturn(1L);

    assertThatThrownBy(() -> service.confirmVerificationCode(EMAIL, "000000"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_OTP_MISMATCH);
    verify(repository, never()).deleteCode(EMAIL);
  }

  @Test
  @DisplayName("5번째로 틀리면 인증번호를 무효화하고 AUTH_OTP_ATTEMPTS_EXCEEDED 예외가 발생한다")
  void confirmVerificationCode_5회실패_무효화() {
    given(repository.findCode(EMAIL)).willReturn(Optional.of("482915"));
    given(repository.increaseAttempts(EMAIL, Duration.ofMinutes(5))).willReturn(5L);

    assertThatThrownBy(() -> service.confirmVerificationCode(EMAIL, "000000"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_OTP_ATTEMPTS_EXCEEDED);
    verify(repository).deleteCode(EMAIL);
  }

  @Test
  @DisplayName("verificationToken 의 이메일과 가입 이메일이 다르면 AUTH_EMAIL_NOT_VERIFIED 예외가 발생한다")
  void consumeVerificationToken_이메일불일치_예외() {
    given(repository.consumeVerifiedEmail(any())).willReturn(Optional.of("other@example.com"));

    assertThatThrownBy(() -> service.consumeVerificationToken("token", EMAIL))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
  }
}
