package com.seoulaim.domain.plan.dto;

import java.time.LocalDate;
import java.util.List;

public record RecommendDatesResponse(List<LocalDate> dates) {}
