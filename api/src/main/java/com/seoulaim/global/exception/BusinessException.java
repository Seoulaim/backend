package com.seoulaim.global.exception;

import java.util.Map;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;
  private final Map<String, String> fields;

  public BusinessException(ErrorCode errorCode) {
    this(errorCode, Map.of());
  }

  /** 에러 응답의 fields 에 추가 정보(예: 결제사 실패 코드)를 담아 내려준다. */
  public BusinessException(ErrorCode errorCode, Map<String, String> fields) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
    this.fields = Map.copyOf(fields);
  }
}
