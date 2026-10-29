package com.seoulaim.global.common;

import com.seoulaim.global.config.AdminProperties;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** /api/v1/admin/** 임시 인증. X-Admin-Key 헤더가 설정된 키와 같아야 통과한다. */
@Component
@RequiredArgsConstructor
public class AdminKeyInterceptor implements HandlerInterceptor {

  public static final String ADMIN_KEY_HEADER = "X-Admin-Key";

  private final AdminProperties adminProperties;

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    String header = request.getHeader(ADMIN_KEY_HEADER);
    if (header == null || !matches(header)) {
      throw new BusinessException(ErrorCode.AUTH_ACCESS_DENIED);
    }
    return true;
  }

  // 타이밍 공격을 피하려고 상수 시간 비교를 쓴다
  private boolean matches(String header) {
    return MessageDigest.isEqual(
        header.getBytes(StandardCharsets.UTF_8),
        adminProperties.key().getBytes(StandardCharsets.UTF_8));
  }
}
