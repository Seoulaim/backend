package com.seoulaim.domain.place.dto;

import com.seoulaim.domain.place.entity.Keyword;
import java.util.UUID;

// 표시 문구는 code 로 클라이언트에서 번역
public record KeywordResponse(UUID id, String code, int displayOrder) {

  public static KeywordResponse from(Keyword keyword) {
    return new KeywordResponse(keyword.getId(), keyword.getCode(), keyword.getDisplayOrder());
  }
}
