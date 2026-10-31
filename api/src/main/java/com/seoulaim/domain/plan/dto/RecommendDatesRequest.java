package com.seoulaim.domain.plan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// artistIds 비어 있으면 내 bias 사용
public record RecommendDatesRequest(
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotNull @Min(1) @Max(3) Integer dayCount,
    @Size(max = 5) List<@NotNull UUID> artistIds) {

  public List<UUID> artistIdsOrEmpty() {
    return artistIds == null ? List.of() : artistIds;
  }
}
