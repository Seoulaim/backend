package com.seoulaim.domain.inquiry.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/** 환불 문의의 처리 상태. API 응답은 소문자. */
public enum RefundStatus {
  REQUESTED,
  REFUNDED,
  REJECTED;

  @JsonValue
  public String apiValue() {
    return name().toLowerCase(Locale.ROOT);
  }
}
