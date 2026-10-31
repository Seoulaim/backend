package com.seoulaim.domain.plan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

// size 미입력 시 20
public record PlanListRequest(String cursor, @Min(1) @Max(50) Integer size) {

  private static final int DEFAULT_SIZE = 20;

  public int sizeOrDefault() {
    return size == null ? DEFAULT_SIZE : size;
  }
}
