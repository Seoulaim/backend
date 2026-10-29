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
public @interface CurrentUserId {}
