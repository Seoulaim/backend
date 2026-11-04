package com.seoulaim.domain.content.admin.dto;

import com.seoulaim.domain.content.entity.CalendarColor;
import com.seoulaim.domain.content.entity.CalendarEntry;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** groupIds: 모든 회원 대상이면 ["all"], 아니면 그룹 id 문자열 (정렬) */
public record AdminCalendarEntryResponse(
    UUID id,
    List<String> groupIds,
    Map<String, String> title,
    LocalDate startDate,
    boolean inCalendar,
    CalendarColor color,
    String imageKey,
    LocalDate endDate,
    Map<String, String> venue,
    String targetUrl,
    int displayOrder,
    Instant createdAt) {

  public static final String ALL_GROUPS = "all";

  public static AdminCalendarEntryResponse from(CalendarEntry entry) {
    List<String> groupIds =
        entry.isAllGroups()
            ? List.of(ALL_GROUPS)
            : entry.getArtistGroupIds().stream().map(UUID::toString).sorted().toList();
    return new AdminCalendarEntryResponse(
        entry.getId(),
        groupIds,
        entry.getTitle(),
        entry.getStartDate(),
        entry.isInCalendar(),
        entry.getColor(),
        entry.getImageKey(),
        entry.getEndDate(),
        entry.getVenue(),
        entry.getTargetUrl(),
        entry.getDisplayOrder(),
        entry.getCreatedAt());
  }
}
