package com.seoulaim.domain.auth.service;

import com.seoulaim.domain.auth.config.EmailVerificationProperties;
import com.seoulaim.domain.auth.dto.ConfirmEmailVerificationResponse;
import com.seoulaim.domain.auth.dto.SendEmailVerificationResponse;
import com.seoulaim.domain.auth.repository.EmailVerificationRedisRepository;
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

  private String generateCode() {
    return "%06d".formatted(RANDOM.nextInt(CODE_BOUND));
  }

  // 응답 시간 차이로 일치 자릿수가 드러나지 않도록 상수 시간 비교
  private boolean matches(String savedCode, String inputCode) {
    return MessageDigest.isEqual(
        savedCode.getBytes(StandardCharsets.UTF_8), inputCode.getBytes(StandardCharsets.UTF_8));
  }
}
