package com.seoulaim.global.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
    ErrorCode errorCode = exception.getErrorCode();

    return ResponseEntity.status(errorCode.getStatus())
        .body(
            new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                exception.getFields(),
                MDC.get("traceId")));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException exception) {
    Map<String, String> fields = new LinkedHashMap<>();

    for (FieldError error : exception.getBindingResult().getFieldErrors()) {
      fields.putIfAbsent(error.getField(), error.getDefaultMessage());
    }

    ErrorCode errorCode = ErrorCode.COMMON_INVALID_REQUEST;

    return ResponseEntity.status(errorCode.getStatus())
        .body(
            new ErrorResponse(
                errorCode.getCode(), errorCode.getMessage(), fields, MDC.get("traceId")));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatchException(
      MethodArgumentTypeMismatchException exception) {
    ErrorCode errorCode = ErrorCode.COMMON_INVALID_REQUEST;

    return ResponseEntity.status(errorCode.getStatus())
        .body(
            new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                Map.of(exception.getName(), "잘못된 값입니다: " + exception.getValue()),
                MDC.get("traceId")));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParameterException(
      MissingServletRequestParameterException exception) {
    ErrorCode errorCode = ErrorCode.COMMON_INVALID_REQUEST;

    return ResponseEntity.status(errorCode.getStatus())
        .body(
            new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                Map.of(exception.getParameterName(), "필수 파라미터입니다"),
                MDC.get("traceId")));
  }

  // JSON 형식 오류, enum 에 없는 값 등 본문 해석 실패
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleNotReadableException(
      HttpMessageNotReadableException exception) {
    ErrorCode errorCode = ErrorCode.COMMON_INVALID_REQUEST;

    return ResponseEntity.status(errorCode.getStatus())
        .body(
            new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                Collections.emptyMap(),
                MDC.get("traceId")));
  }

  // 등록되지 않은 경로 (정적 리소스 핸들러까지 못 찾으면 NoResourceFoundException)
  @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
  public ResponseEntity<ErrorResponse> handleNotFoundException(Exception exception) {
    ErrorCode errorCode = ErrorCode.COMMON_NOT_FOUND;

    return ResponseEntity.status(errorCode.getStatus())
        .body(
            new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                Collections.emptyMap(),
                MDC.get("traceId")));
  }

  // 경로는 있지만 메서드가 다름. Allow 헤더에 지원 메서드를 담는다
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(
      HttpRequestMethodNotSupportedException exception) {
    ErrorCode errorCode = ErrorCode.COMMON_METHOD_NOT_ALLOWED;
    HttpHeaders headers = new HttpHeaders();
    if (exception.getSupportedHttpMethods() != null) {
      headers.setAllow(exception.getSupportedHttpMethods());
    }

    return ResponseEntity.status(errorCode.getStatus())
        .headers(headers)
        .body(
            new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                Collections.emptyMap(),
                MDC.get("traceId")));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception exception) {
    ErrorCode errorCode = ErrorCode.COMMON_INTERNAL_ERROR;

    return ResponseEntity.status(errorCode.getStatus())
        .body(
            new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                Collections.emptyMap(),
                MDC.get("traceId")));
  }
}
