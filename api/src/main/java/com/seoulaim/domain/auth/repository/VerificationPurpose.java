package com.seoulaim.domain.auth.repository;

/**
 * 이메일 인증번호 용도. 용도마다 Redis 키 공간이 달라 가입 인증번호로 비밀번호를 재설정할 수 없다
 *
 * <ul>
 *   <li>SIGNUP: 기존 회원가입 키(auth:email-verification:)를 그대로 사용
 *   <li>PASSWORD_RESET: 비밀번호 재설정
 * </ul>
 */
public enum VerificationPurpose {
  SIGNUP("auth:email-verification:"),
  PASSWORD_RESET("auth:password-reset:");

  private final String keyPrefix;

  VerificationPurpose(String keyPrefix) {
    this.keyPrefix = keyPrefix;
  }

  String codeKey(String email) {
    return keyPrefix + "code:" + email;
  }

  String attemptsKey(String email) {
    return keyPrefix + "attempts:" + email;
  }

  String cooldownKey(String email) {
    return keyPrefix + "cooldown:" + email;
  }
}
