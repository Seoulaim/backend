package com.seoulaim.domain.auth.service;

import com.seoulaim.domain.auth.config.EmailVerificationProperties;
import com.seoulaim.domain.auth.dto.ConfirmEmailVerificationResponse;
import com.seoulaim.domain.auth.dto.SendEmailVerificationResponse;
import com.seoulaim.domain.auth.repository.EmailVerificationRedisRepository;
import com.seoulaim.domain.auth.repository.VerificationPurpose;
import com.seoulaim.domain.user.service.UserService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.mail.EmailSender;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 전 이메일 인증
 *
 * <ul>
 *   <li>인증번호 발송 → 확인 → verificationToken 발급
 *   <li>회원가입 시 verificationToken 1회 소모
 *   <li>비밀번호 재설정: 같은 방식의 인증번호를 PASSWORD_RESET 키 공간에 저장, 재설정 요청에서 바로 확인·소모
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EmailVerificationService {

  private static final int CODE_BOUND = 1_000_000;
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String MAIL_SUBJECT = "[Seoulaim] Email verification code";
  private static final String MAIL_TEXT =
      "Your Seoulaim verification code is %s.%nThis code expires in %d minutes.";
  private static final String RESET_MAIL_SUBJECT = "[Seoulaim] Password reset code";
  private static final String RESET_MAIL_TEXT =
      "Your Seoulaim password reset code is %s.%nThis code expires in %d minutes."
          + "%nIf you did not request this, you can ignore this email.";

  private final EmailVerificationRedisRepository emailVerificationRedisRepository;
  private final UserService userService;
  private final EmailSender emailSender;
  private final EmailVerificationProperties properties;

  public SendEmailVerificationResponse sendVerificationCode(String rawEmail) {
    String email = EmailNormalizer.normalize(rawEmail);
    if (userService.existsByEmail(email)) {
      throw new BusinessException(ErrorCode.USER_EMAIL_DUPLICATED);
    }
    if (!emailVerificationRedisRepository.tryAcquireResendCooldown(
        email, properties.resendCooldown())) {
      throw new BusinessException(ErrorCode.AUTH_OTP_RESEND_TOO_SOON);
    }

    String code = generateCode();
    emailVerificationRedisRepository.saveCode(email, code, properties.codeTtl());

    try {
      emailSender.send(
          email, MAIL_SUBJECT, MAIL_TEXT.formatted(code, properties.codeTtl().toMinutes()));
    } catch (BusinessException exception) {
      // 발송 실패 → 바로 재시도 가능하도록 인증번호·재발송 제한 해제
      emailVerificationRedisRepository.deleteCode(email);
      emailVerificationRedisRepository.deleteResendCooldown(email);
      throw exception;
    }

    return new SendEmailVerificationResponse(
        properties.codeTtl().toSeconds(), properties.resendCooldown().toSeconds());
  }

  public ConfirmEmailVerificationResponse confirmVerificationCode(String rawEmail, String code) {
    String email = EmailNormalizer.normalize(rawEmail);
    String savedCode =
        emailVerificationRedisRepository
            .findCode(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_OTP_EXPIRED));

    if (!matches(savedCode, code)) {
      long attempts =
          emailVerificationRedisRepository.increaseAttempts(email, properties.codeTtl());
      if (attempts >= properties.maxAttempts()) {
        emailVerificationRedisRepository.deleteCode(email);
        throw new BusinessException(ErrorCode.AUTH_OTP_ATTEMPTS_EXCEEDED);
      }
      throw new BusinessException(ErrorCode.AUTH_OTP_MISMATCH);
    }

    emailVerificationRedisRepository.deleteCode(email);
    String verificationToken = UUID.randomUUID().toString();
    emailVerificationRedisRepository.saveVerifiedEmail(
        verificationToken, email, properties.verificationTokenTtl());

    return new ConfirmEmailVerificationResponse(
        verificationToken, properties.verificationTokenTtl().toSeconds());
  }

  // 회원가입에서 호출. 토큰 없음·만료·이메일 불일치 → AUTH_EMAIL_NOT_VERIFIED
  public void consumeVerificationToken(String verificationToken, String rawEmail) {
    String email = EmailNormalizer.normalize(rawEmail);
    String verifiedEmail =
        emailVerificationRedisRepository
            .consumeVerifiedEmail(verificationToken)
            .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED));
    if (!verifiedEmail.equals(email)) {
      throw new BusinessException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
    }
  }

  /**
   * 비밀번호 재설정 인증번호 발송. 가입 여부를 드러내지 않도록 미가입·소셜 전용 이메일도 같은 응답 (메일만 안 보냄)
   *
   * <p>재발송 제한도 가입 여부와 무관하게 먼저 적용 → 429 여부로도 가입 여부를 알 수 없음
   */
  public SendEmailVerificationResponse sendPasswordResetCode(String rawEmail) {
    String email = EmailNormalizer.normalize(rawEmail);
    VerificationPurpose purpose = VerificationPurpose.PASSWORD_RESET;
    if (!emailVerificationRedisRepository.tryAcquireResendCooldown(
        purpose, email, properties.resendCooldown())) {
      throw new BusinessException(ErrorCode.AUTH_OTP_RESEND_TOO_SOON);
    }

    boolean hasPassword =
        userService.findByEmail(email).filter(user -> user.getPasswordHash() != null).isPresent();
    if (hasPassword) {
      String code = generateCode();
      emailVerificationRedisRepository.saveCode(purpose, email, code, properties.codeTtl());
      try {
        emailSender.send(
            email,
            RESET_MAIL_SUBJECT,
            RESET_MAIL_TEXT.formatted(code, properties.codeTtl().toMinutes()));
      } catch (BusinessException exception) {
        emailVerificationRedisRepository.deleteCode(purpose, email);
        emailVerificationRedisRepository.deleteResendCooldown(purpose, email);
        throw exception;
      }
    }

    return new SendEmailVerificationResponse(
        properties.codeTtl().toSeconds(), properties.resendCooldown().toSeconds());
  }

  /**
   * 비밀번호 재설정 인증번호 확인 후 1회 소모. 가입 인증번호(SIGNUP)는 키 공간이 달라 통과하지 못함
   *
   * <p>없음·만료 → AUTH_OTP_EXPIRED, 불일치 → AUTH_OTP_MISMATCH, 최대 시도 초과 → AUTH_OTP_ATTEMPTS_EXCEEDED
   */
  public void consumePasswordResetCode(String rawEmail, String code) {
    String email = EmailNormalizer.normalize(rawEmail);
    VerificationPurpose purpose = VerificationPurpose.PASSWORD_RESET;
    String savedCode =
        emailVerificationRedisRepository
            .findCode(purpose, email)
            .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_OTP_EXPIRED));

    if (!matches(savedCode, code)) {
      long attempts =
          emailVerificationRedisRepository.increaseAttempts(purpose, email, properties.codeTtl());
      if (attempts >= properties.maxAttempts()) {
        emailVerificationRedisRepository.deleteCode(purpose, email);
        throw new BusinessException(ErrorCode.AUTH_OTP_ATTEMPTS_EXCEEDED);
      }
      throw new BusinessException(ErrorCode.AUTH_OTP_MISMATCH);
    }
    emailVerificationRedisRepository.deleteCode(purpose, email);
  }

  private String generateCode() {
    return "%06d".formatted(RANDOM.nextInt(CODE_BOUND));
  }

  // 응답 시간 차이로 일치 자릿수가 드러나지 않도록 상수 시간 비교
  private boolean matches(String savedCode, String inputCode) {
    return MessageDigest.isEqual(
        savedCode.getBytes(StandardCharsets.UTF_8), inputCode.getBytes(StandardCharsets.UTF_8));
  }
}
