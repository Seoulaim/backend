package com.seoulaim.global.common;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** {@link ClientPlatform} 타입 파라미터에 X-Client-Platform 헤더 값을 넣는다. 항상 null 이 아니다. */
public class ClientPlatformArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return ClientPlatform.class.equals(parameter.getParameterType());
  }

  @Override
  public ClientPlatform resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    return ClientPlatform.from(webRequest.getHeader(ClientPlatform.HEADER));
  }
}
