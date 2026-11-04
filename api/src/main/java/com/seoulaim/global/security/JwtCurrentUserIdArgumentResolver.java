package com.seoulaim.global.security;

import com.seoulaim.global.common.CurrentUserId;
import com.seoulaim.global.common.CurrentUserIdArgumentResolver;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link com.seoulaim.global.common.CurrentUserId} 리졸버
 *
 * <ul>
 *   <li>JwtAuthenticationFilter 가 SecurityContext 에 넣은 userId 를 반환
 *   <li>인증 정보 없음(비로그인·공개 경로) → AUTH_INVALID_TOKEN
 *   <li>{@code required = false}: 헤더 없음 → null, 토큰 오류(만료 등) → 그 사유 코드로 401
 * </ul>
 */
@Component
public class JwtCurrentUserIdArgumentResolver extends CurrentUserIdArgumentResolver {

  @Override
  public UUID resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    // 익명 사용자는 principal 이 문자열("anonymousUser") → UUID 가 아니면 미인증
    if (authentication != null && authentication.getPrincipal() instanceof UUID userId) {
      return userId;
    }
    CurrentUserId annotation = parameter.getParameterAnnotation(CurrentUserId.class);
    if (annotation != null && !annotation.required()) {
      // 공개 경로라 필터가 통과시킨 토큰 오류 → 조용히 비로그인 취급하지 않고 401
      Object authError =
          webRequest.getAttribute(
              JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
      if (authError instanceof ErrorCode errorCode) {
        throw new BusinessException(errorCode);
      }
      return null;
    }
    throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
  }
}
