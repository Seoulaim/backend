package com.seoulaim.domain.inquiry.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/** 접수됨 → 답변(메일) 또는 환불 결정 후 ANSWERED. API 응답은 소문자. */
public enum InquiryStatus {
  RECEIVED,
  ANSWERED;

  @JsonValue
  public String apiValue() {
    return name().toLowerCase(Locale.ROOT);
  }
}
