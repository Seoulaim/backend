package com.seoulaim.global.common;

import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * prod 에서 헤더 기반 식별을 막는 구현. JWT 기반 구현이 들어오기 전까지 항상 401 을 반환한다.
 *
 * <p>리졸버가 아예 없으면 Spring 이 UUID 파라미터를 쿼리스트링(?userId=)으로 바인딩해 유저를 위조할 수 있다.
 */
@Component
@Profile("prod")
public class DisabledCurrentUserIdArgumentResolver extends CurrentUserIdArgumentResolver {

  @Override
  public UUID resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
  }
}
