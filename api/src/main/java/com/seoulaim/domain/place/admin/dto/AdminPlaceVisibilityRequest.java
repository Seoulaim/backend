package com.seoulaim.domain.place.admin.dto;

import jakarta.validation.constraints.NotNull;

// 노출 여부만 변경 (다른 필드 검증 없음)
public record AdminPlaceVisibilityRequest(@NotNull Boolean visible) {}
