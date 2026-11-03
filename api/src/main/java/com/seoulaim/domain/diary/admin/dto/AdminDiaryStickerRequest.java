package com.seoulaim.domain.diary.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * 스티커 생성·수정 공용.
 *
 * <ul>
 *   <li>imageFileId 생략: 생성은 400 COMMON_INVALID_REQUEST, 수정은 기존 이미지 유지
 *   <li>isActive 생략: 생성은 true, 수정은 기존 값 유지
 * </ul>
 */
public record AdminDiaryStickerRequest(
    @Size(max = 100) String name,
    UUID imageFileId,
    @NotNull Integer displayOrder,
    Boolean isActive) {}
