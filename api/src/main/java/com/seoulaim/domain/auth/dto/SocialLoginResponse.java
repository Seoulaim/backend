package com.seoulaim.domain.auth.dto;

/**
 * 소셜 로그인 결과
 *
 * <ul>
 *   <li>signupRequired=false: 기존 회원 → tokens 사용, email·suggestedNickname null
 *   <li>signupRequired=true: 신규 → 약관 동의·닉네임 입력 후 소셜 가입 API 호출, tokens null
 *   <li>email: 제공자가 인증한 이메일, 없으면 null
 *   <li>suggestedNickname: 닉네임 입력칸 기본값 (제공자 이름, 최대 20자)
 * </ul>
 */
public record SocialLoginResponse(
    boolean signupRequired, String email, String suggestedNickname, TokenResponse tokens) {

  public static SocialLoginResponse loggedIn(TokenResponse tokens) {
    return new SocialLoginResponse(false, null, null, tokens);
  }

  public static SocialLoginResponse signupRequired(String email, String suggestedNickname) {
    return new SocialLoginResponse(true, email, suggestedNickname, null);
  }
}
