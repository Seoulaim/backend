package com.seoulaim.domain.place.dto;

import com.seoulaim.domain.place.entity.Keyword;
import com.seoulaim.domain.place.entity.LocalizedText;
import java.util.UUID;

// names: 언어별 이름, 빈 언어는 null (영어로 표시)
public record KeywordResponse(UUID id, String code, LocalizedText names, int displayOrder) {

  public static KeywordResponse from(Keyword keyword) {
    return new KeywordResponse(
        keyword.getId(), keyword.getCode(), keyword.getNames(), keyword.getDisplayOrder());
  }
}
