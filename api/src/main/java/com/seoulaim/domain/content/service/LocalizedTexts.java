package com.seoulaim.domain.content.service;

import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 어드민이 입력한 다국어 문구 { en, ko, zh, ja } 정리·검증. 값은 strip, 빈 값은 뺀다. */
final class LocalizedTexts {

  private static final Set<String> LANGUAGES = Set.of("en", "ko", "zh", "ja");
  private static final String ENGLISH = "en";

  private LocalizedTexts() {}

  /** en 필수. 없거나 비었으면 400 (fields.{field}). */
  static Map<String, String> required(Map<String, String> texts, String field) {
    Map<String, String> normalized = optional(texts, field);
    if (!normalized.containsKey(ENGLISH)) {
      throw invalid(field, "영어(en) 문구는 필수입니다");
    }
    return normalized;
  }

  /** null 이면 빈 맵. 모르는 언어 키는 400. */
  static Map<String, String> optional(Map<String, String> texts, String field) {
    if (texts == null) {
      return Map.of();
    }
    Map<String, String> normalized = new LinkedHashMap<>();
    texts.forEach(
        (language, value) -> {
          if (!LANGUAGES.contains(language)) {
            throw invalid(field, "en, ko, zh, ja 만 쓸 수 있습니다");
          }
          if (value != null && !value.isBlank()) {
            normalized.put(language, value.strip());
          }
        });
    return normalized;
  }

  /** 비면 null (선택 jsonb 컬럼용). */
  static Map<String, String> optionalOrNull(Map<String, String> texts, String field) {
    Map<String, String> normalized = optional(texts, field);
    return normalized.isEmpty() ? null : normalized;
  }

  static BusinessException invalid(String field, String reason) {
    return new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, Map.of(field, reason));
  }
}
