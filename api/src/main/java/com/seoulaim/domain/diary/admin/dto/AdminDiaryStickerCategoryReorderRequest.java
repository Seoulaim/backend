package com.seoulaim.domain.diary.admin.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/** ids 순서대로 앞에서부터 배치한다. 빠진 카테고리는 기존 순서를 유지한 채 뒤로 간다. */
public record AdminDiaryStickerCategoryReorderRequest(@NotNull List<@NotNull UUID> ids) {}
