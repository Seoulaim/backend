package com.seoulaim.domain.plan.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePlanRequest(
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotNull @Size(min = 1, max = 3) List<@NotNull LocalDate> visitDates,
    @NotNull @Size(min = 1, max = 5) List<@NotNull UUID> artistIds,
    @Size(max = 10) List<@NotNull UUID> keywordIds) {

  public List<UUID> keywordIdsOrEmpty() {
    return keywordIds == null ? List.of() : keywordIds;
  }
}
