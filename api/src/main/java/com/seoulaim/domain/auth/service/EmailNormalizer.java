package com.seoulaim.domain.auth.service;

import java.util.Locale;

/** 이메일 비교·저장 기준 통일 (앞뒤 공백 제거, 소문자) */
public final class EmailNormalizer {

  private EmailNormalizer() {}

  public static String normalize(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
