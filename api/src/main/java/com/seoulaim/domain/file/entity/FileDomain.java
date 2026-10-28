package com.seoulaim.domain.file.entity;

import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.Arrays;
import java.util.Locale;

/** 파일을 사용하는 도메인. s3 key 의 첫 경로 세그먼트(소문자)가 된다. */
public enum FileDomain {
  REVIEW,
  PLAN,
  PROFILE,
  PRODUCT,
  DIARY,
  INQUIRY;

  public static FileDomain from(String value) {
    return Arrays.stream(values())
        .filter(domain -> domain.name().equals(value))
        .findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.FILE_INVALID_DOMAIN));
  }

  public String keyPrefix() {
    return name().toLowerCase(Locale.ROOT);
  }
}
