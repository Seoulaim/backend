package com.seoulaim.domain.content.dto;

import com.seoulaim.domain.content.entity.CalendarEntry;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/** Event coming up 카드 (HH01). targetType 은 항상 "URL". */
public record HomeEventResponse(
    UUID id,
    Map<String, String> title,
    LocalDate startDate,
    LocalDate endDate,
    Map<String, String> venue,
    String imageKey,
    String targetType,
    String targetUrl) {

  private static final String TARGET_TYPE_URL = "URL";

  public static HomeEventResponse from(CalendarEntry entry) {
    return new HomeEventResponse(
        entry.getId(),
        entry.getTitle(),
        entry.getStartDate(),
        entry.getEndDate(),
        entry.getVenue(),
        entry.getImageKey(),
        TARGET_TYPE_URL,
        entry.getTargetUrl());
  }
}
