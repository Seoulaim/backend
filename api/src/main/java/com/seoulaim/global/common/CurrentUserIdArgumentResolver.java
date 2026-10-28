package com.seoulaim.global.common;

import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

/** {@link CurrentUserId} 파라미터 리졸버의 공통 부분. 실제 유저 식별 방식은 하위 구현이 정한다. */
public abstract class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(CurrentUserId.class)
        && UUID.class.equals(parameter.getParameterType());
  }
}
