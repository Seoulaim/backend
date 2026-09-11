package com.seoulaim.global.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
