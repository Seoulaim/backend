package com.seoulaim.domain.content.admin.dto;

import com.seoulaim.domain.content.entity.BannerPlacement;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/** ids: 그 위치의 배너 전부(기본 배너 포함)를 새 순서로. 맨 앞이 먼저 보인다. */
public record AdminBannerReorderRequest(
    @NotNull BannerPlacement placement, @NotNull List<@NotNull UUID> ids) {}
