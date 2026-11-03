package com.seoulaim.domain.place.admin.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

// 운영 기간 (시작일·종료일 당일 포함). 상시 운영이면 이 객체 대신 null
public record AdminPlaceOperatingPeriod(@NotNull LocalDate startsOn, @NotNull LocalDate endsOn) {}
