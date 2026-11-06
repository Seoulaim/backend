package com.seoulaim.domain.user.service;

/**
 * 목록용 이메일 마스킹. 어드민 lib/privacy.js 의 maskEmail 과 같은 규칙.
 *
 * <ul>
 *   <li>로컬파트 1~2자: 첫 1자 + ** ("a@x.com" → "a**@x.com", "ab@x.com" → "a**@x.com")
 *   <li>3자 이상: 앞 2자 + 나머지 길이만큼 *, 최소 2개 ("abc@" → "ab**@", "abcde@" → "ab***@")
 * </ul>
 */
public final class EmailMasker {

  private static final int MIN_STARS = 2;

  private EmailMasker() {}

  /** null 이면 null (탈퇴 회원). '@' 가 없으면 전체를 로컬파트로 본다. */
  public static String mask(String email) {
    if (email == null) {
      return null;
    }
    int at = email.lastIndexOf('@');
    String local = at < 0 ? email : email.substring(0, at);
    String domain = at < 0 ? "" : email.substring(at);
    int shown = Math.min(2, Math.max(1, local.length() - 1));
    shown = Math.min(shown, local.length());
    int stars = Math.max(MIN_STARS, local.length() - shown);
    return local.substring(0, shown) + "*".repeat(stars) + domain;
  }
}
