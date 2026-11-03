package com.seoulaim.domain.place.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.seoulaim.domain.place.entity.PlaceOpeningHour;
import com.seoulaim.domain.place.entity.Weekday;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

// 요일별 영업시간. 휴무일은 시각 모두 null
public record PlaceOpeningHourResponse(
    Weekday dayOfWeek,
    boolean closed,
    @JsonFormat(pattern = "HH:mm") LocalTime opensAt,
    @JsonFormat(pattern = "HH:mm") LocalTime closesAt,
    @JsonFormat(pattern = "HH:mm") LocalTime breakStartsAt,
    @JsonFormat(pattern = "HH:mm") LocalTime breakEndsAt) {

  public static PlaceOpeningHourResponse from(PlaceOpeningHour hour) {
    return new PlaceOpeningHourResponse(
        hour.getDayOfWeek(),
        hour.isClosed(),
        hour.getOpensAt(),
        hour.getClosesAt(),
        hour.getBreakStartsAt(),
        hour.getBreakEndsAt());
  }

  // 월 ~ 일 순. 영업시간 없음(빈 목록)이면 null
  public static List<PlaceOpeningHourResponse> listOf(List<PlaceOpeningHour> hours) {
    if (hours.isEmpty()) {
      return null;
    }
    return hours.stream()
        .sorted(Comparator.comparing(PlaceOpeningHour::getDayOfWeek))
        .map(PlaceOpeningHourResponse::from)
        .toList();
  }
}
