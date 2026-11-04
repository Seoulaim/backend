package com.seoulaim.domain.place.admin.dto;

import com.seoulaim.domain.place.entity.LocalizedText;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 키워드 등록·수정 공용
 *
 * <ul>
 *   <li>code: 등록 시에만 사용, 생략하면 영어 이름으로 생성 (예: "Night View" → NIGHT_VIEW). 수정 시 무시
 *   <li>name: 영어 필수, 나머지 언어는 비면 영어로 표시
 * </ul>
 */
public record AdminKeywordRequest(
    @Size(max = 50) @Pattern(regexp = "^[A-Z0-9_]+$") String code,
    @NotNull @Valid AdminKeywordName name) {

  public record AdminKeywordName(
      @NotBlank @Size(max = 100) String en,
      @Size(max = 100) String ko,
      @Size(max = 100) String ja,
      @Size(max = 100) String zh) {

    public LocalizedText toLocalized() {
      return new LocalizedText(en.trim(), blankToNull(ko), blankToNull(ja), blankToNull(zh));
    }
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
