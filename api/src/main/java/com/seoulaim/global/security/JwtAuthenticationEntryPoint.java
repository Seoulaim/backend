package com.seoulaim.global.security;

import com.seoulaim.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** 인증 필요 API 에 인증 없이 접근 → 401. 필터가 기록한 사유(만료·위조), 없으면 토큰 누락으로 판단 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final SecurityErrorResponseWriter errorResponseWriter;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {

    Object recorded = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
    ErrorCode errorCode = recorded instanceof ErrorCode code ? code : ErrorCode.AUTH_INVALID_TOKEN;

    errorResponseWriter.write(response, errorCode);
  }
}
