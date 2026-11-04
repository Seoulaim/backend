package com.seoulaim.global.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 현재 요청 유저의 ID(UUID)를 컨트롤러 파라미터로 주입
 *
 * <p>값은 JWT access token 에서 추출 (JwtCurrentUserIdArgumentResolver)
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {

  /**
   * false 면 선택적 인증 (공개 경로에서 로그인 회원만 개인화할 때)
   *
   * <ul>
   *   <li>Authorization 헤더 없음 → null
   *   <li>유효한 토큰 → userId
   *   <li>토큰이 있는데 만료·위조 → 401 (앱이 토큰을 갱신하고 다시 요청하도록)
   * </ul>
   */
  boolean required() default true;
}
