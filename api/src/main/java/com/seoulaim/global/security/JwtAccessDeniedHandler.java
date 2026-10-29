package com.seoulaim.global.security;

import com.seoulaim.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** 인증은 됐지만 권한 없음 → 403 */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

  private final SecurityErrorResponseWriter errorResponseWriter;

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    errorResponseWriter.write(response, ErrorCode.AUTH_ACCESS_DENIED);
  }
}
