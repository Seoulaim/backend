package com.seoulaim.domain.place.admin.dto;

import java.util.List;

// totalCount: 필터 조건에 맞는 전체 개수, nextCursor: 마지막 페이지면 null
public record AdminPlaceListResponse(
    List<AdminPlaceSummaryResponse> items, long totalCount, String nextCursor) {}
