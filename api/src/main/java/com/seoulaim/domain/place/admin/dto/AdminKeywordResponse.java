package com.seoulaim.domain.place.admin.dto;

import com.seoulaim.domain.place.entity.Keyword;
import com.seoulaim.domain.place.entity.LocalizedText;
import java.util.UUID;

// placeCount: 이 키워드가 연결된 장소 수 (숨김 장소 포함)
public record AdminKeywordResponse(
    UUID id, String code, LocalizedText name, int displayOrder, long placeCount) {

  public static AdminKeywordResponse of(Keyword keyword, long placeCount) {
    return new AdminKeywordResponse(
        keyword.getId(),
        keyword.getCode(),
        keyword.getNames(),
        keyword.getDisplayOrder(),
        placeCount);
  }
}
