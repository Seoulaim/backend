package com.seoulaim.domain.content.dto;

import com.seoulaim.domain.content.entity.CalendarColor;
import com.seoulaim.domain.content.entity.CalendarEntry;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/** 팬캘린더 일정 (HH01). date = 항목의 시작일, color 는 소문자(pink 등). */
public record FanScheduleResponse(
    UUID id, LocalDate date, Map<String, String> title, CalendarColor color) {

  public static FanScheduleResponse from(CalendarEntry entry) {
    return new FanScheduleResponse(
        entry.getId(), entry.getStartDate(), entry.getTitle(), entry.getColor());
  }
}
