package com.seoulaim.domain.diary.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;

/** 다이어리 다운로드 형식. 요청은 대소문자 무시, 응답은 대문자. */
public enum DiaryDownloadFormat {
  PDF,
  JPG;

  /** 없는 값이면 null → 역직렬화 후 @NotNull 로 400. */
  @JsonCreator
  public static DiaryDownloadFormat parse(String value) {
    if (value == null) {
      return null;
    }
    return Arrays.stream(values())
        .filter(format -> format.name().equalsIgnoreCase(value.strip()))
        .findFirst()
        .orElse(null);
  }
}
