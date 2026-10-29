package com.seoulaim.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulaim.global.config.TraceIdFilter;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/** 시큐리티 필터 단계 에러를 GlobalExceptionHandler 와 같은 ErrorResponse 형식으로 응답 */
@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

  private final ObjectMapper objectMapper;

  public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
    response.setStatus(errorCode.getStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    objectMapper.writeValue(
        response.getWriter(),
        new ErrorResponse(
            errorCode.getCode(),
            errorCode.getMessage(),
            Map.of(),
            MDC.get(TraceIdFilter.TRACE_ID)));
  }
}
