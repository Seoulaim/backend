package com.seoulaim.global.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 현재 요청 유저의 ID(UUID)를 컨트롤러 파라미터로 주입한다.
 *
 * <p>auth 미구현 동안은 X-User-Id 헤더를 읽는다. JWT 필터가 들어오면 리졸버 구현만 교체한다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {}
