package com.seoulaim.global.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
                Collections.emptyMap(),
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
