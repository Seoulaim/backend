package com.seoulaim.domain.content.admin.dto;

import jakarta.validation.constraints.NotNull;

public record AdminBannerEnabledRequest(@NotNull Boolean enabled) {}
