package com.seoulaim.domain.content.admin.dto;

import com.seoulaim.domain.content.entity.CalendarColor;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 팬캘린더·Event coming up 항목 생성·수정 공용 (어드민 events.js calendar entry 와 같은 필드명)
 *
 * <ul>
 *   <li>groupIds: 그룹 id 1개 이상, 또는 ["all"](모든 회원). "all" 이 있으면 나머지는 무시
 *   <li>title·venue: { en, ko, zh, ja }, title 의 en 필수
 *   <li>inCalendar 이면 color 필수. inCalendar 가 아니면 사진 필수 (어디에도 안 보이므로)
 *   <li>사진이 있으면 endDate 필수 (Event coming up 노출 기간)
 *   <li>imageFileId 생략: 생성은 사진 없음, 수정은 기존 유지. removeImage=true 면 사진 제거 (imageFileId 와 동시 전송 불가)
 * </ul>
 */
public record AdminCalendarEntryRequest(
    @NotEmpty List<@NotNull String> groupIds,
    @NotNull Map<String, String> title,
    @NotNull LocalDate startDate,
    @NotNull Boolean inCalendar,
    CalendarColor color,
    UUID imageFileId,
    Boolean removeImage,
    LocalDate endDate,
    Map<String, String> venue,
    @Size(max = 1000) String targetUrl) {}
