package com.seoulaim.domain.artist.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

// 빈 배열 = 전체 해제, 배열 순서 = 노출 순서
public record UpdateUserBiasesRequest(
    @NotNull @Size(max = 5, message = "최대 5명까지 선택할 수 있습니다") List<@NotNull UUID> artistIds) {}
