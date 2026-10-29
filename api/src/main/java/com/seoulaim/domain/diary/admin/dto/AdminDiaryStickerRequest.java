package com.seoulaim.domain.diary.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** 스티커 생성·수정 공용. isActive 를 생략하면 생성은 true, 수정은 기존 값을 유지한다. */
public record AdminDiaryStickerRequest(
    @Size(max = 100) String name,
    @NotNull UUID imageFileId,
    @NotNull Integer displayOrder,
    Boolean isActive) {}
