package com.seoulaim.domain.place.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

// 전체 키워드 id 를 노출 순서대로. 빠지거나 중복된 id 가 있으면 400
public record AdminKeywordOrderRequest(@NotEmpty List<@NotNull UUID> keywordIds) {}
