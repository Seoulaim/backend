package com.seoulaim.global.util;

import java.util.Locale;

/** JPQL LIKE 부분 일치 패턴. 쿼리에는 {@code escape '\'} 를 붙인다. */
public final class LikePattern {

  private LikePattern() {}

  /** 앞뒤 공백 제거·소문자·특수문자(\ % _) 이스케이프한 %keyword%. lower(컬럼) 과 비교한다. */
  public static String containing(String keyword) {
    String escaped =
        keyword
            .strip()
            .toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    return "%" + escaped + "%";
  }
}
