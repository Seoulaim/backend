package com.seoulaim.domain.plan.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PlanSummaryResponse(
    UUID id,
    String title,
    LocalDate startDate,
    LocalDate endDate,
    int dayCount,
    Instant createdAt) {}
