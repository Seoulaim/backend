package com.seoulaim.domain.inquiry.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** 문의 유형 (MR03). DB 는 대문자, API 응답은 소문자(어드민·회원 앱 목업과 같은 값). */
public enum InquiryCategory {
  REFUND,
  PAYMENT,
  DIARY,
  ACCOUNT,
  REPORT,
  OTHER;

  /** 대소문자 무시. 없는 값이면 empty. */
  public static Optional<InquiryCategory> parse(String value) {
    if (value == null) {
      return Optional.empty();
    }
    return Arrays.stream(values())
        .filter(c -> c.name().equalsIgnoreCase(value.strip()))
        .findFirst();
  }

  @JsonValue
  public String apiValue() {
    return name().toLowerCase(Locale.ROOT);
  }
}
