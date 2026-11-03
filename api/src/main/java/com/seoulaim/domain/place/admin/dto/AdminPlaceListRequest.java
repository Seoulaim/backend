package com.seoulaim.domain.place.admin.dto;

import com.seoulaim.domain.place.entity.PlaceCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

/**
 * 어드민 장소 목록 필터 (모두 선택)
 *
 * <ul>
 *   <li>keyword: 장소명 부분 일치 (대소문자 무시)
 *   <li>visible: true 노출 중, false 숨김
 *   <li>artistGroupId: 해당 그룹 또는 그 멤버 태그가 있는 장소
 *   <li>untagged: true 면 멤버 태그 없는 장소만 (artistGroupId 와 함께 사용 불가)
 *   <li>size 미입력 시 20
 * </ul>
 */
public record AdminPlaceListRequest(
    String keyword,
    Boolean visible,
    PlaceCategory category,
    UUID artistGroupId,
    Boolean untagged,
    String cursor,
    @Min(1) @Max(50) Integer size) {

  private static final int DEFAULT_SIZE = 20;

  public int sizeOrDefault() {
    return size == null ? DEFAULT_SIZE : size;
  }

  public boolean untaggedOnly() {
    return Boolean.TRUE.equals(untagged);
  }
}
