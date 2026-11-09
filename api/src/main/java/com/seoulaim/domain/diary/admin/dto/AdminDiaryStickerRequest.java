package com.seoulaim.domain.diary.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * 스티커 생성·수정 공용.
 *
 * <ul>
 *   <li>imageFileId 생략: 생성은 400 COMMON_INVALID_REQUEST, 수정은 기존 이미지 유지
 *   <li>isActive 생략: 생성은 true, 수정은 기존 값 유지
 *   <li>categoryIds 생략: 생성은 카테고리 없음, 수정은 기존 연결 유지. 빈 배열이면 전부 해제. 없는 id 는 404
 *       DIARY_STICKER_CATEGORY_NOT_FOUND, 중복 id 는 하나로 합친다
 * </ul>
 */
public record AdminDiaryStickerRequest(
    @Size(max = 100) String name,
    UUID imageFileId,
    @NotNull Integer displayOrder,
    Boolean isActive,
    List<@NotNull UUID> categoryIds) {

  /** categoryIds 없이 만드는 기존 형태 (하위 호환). */
  public AdminDiaryStickerRequest(
      String name, UUID imageFileId, Integer displayOrder, Boolean isActive) {
    this(name, imageFileId, displayOrder, isActive, null);
  }
}
