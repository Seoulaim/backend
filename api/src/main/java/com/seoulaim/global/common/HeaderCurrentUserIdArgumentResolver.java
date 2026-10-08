package com.seoulaim.global.common;

import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

/** auth 미구현 동안 쓰는 임시 구현. X-User-Id 헤더를 UUID 로 읽는다. */
@Component
@Profile("!prod")
public class HeaderCurrentUserIdArgumentResolver extends CurrentUserIdArgumentResolver {

  public static final String USER_ID_HEADER = "X-User-Id";

  @Override
  public UUID resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    String header = webRequest.getHeader(USER_ID_HEADER);
    if (!StringUtils.hasText(header)) {
      throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
    }
    try {
      return UUID.fromString(header.trim());
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
    }
  }
}
