package com.seoulaim.domain.auth.service;

import java.util.regex.Pattern;

/** 비밀번호 규칙. SignupRequest 의 @Pattern 과 같은 정규식 (8~64자, 영문·숫자 각 1자 이상) */
final class PasswordPolicy {

  private static final Pattern PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,64}$");

  private PasswordPolicy() {}

  static boolean isValid(String password) {
    return password != null && PATTERN.matcher(password).matches();
  }
}
