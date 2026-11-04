package com.seoulaim.domain.content.admin.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * 생일카페 생성·수정 공용
 *
 * <ul>
 *   <li>artistId: 생일인 멤버. 어드민 mock 의 memberId 로 보내도 받는다
 *   <li>venue: { en, ko, zh, ja }, en 필수
 *   <li>imageFileId 생략: 생성은 사진 없음, 수정은 기존 유지. removeImage=true 면 사진 제거
 * </ul>
 */
public record AdminBirthdayCafeRequest(
    @NotNull @JsonAlias("memberId") UUID artistId,
    @NotNull Map<String, String> venue,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    UUID imageFileId,
    Boolean removeImage,
    @Size(max = 1000) String linkUrl) {}
