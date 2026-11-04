package com.seoulaim.domain.content.dto;

import com.seoulaim.domain.content.entity.BirthdayCafe;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/** 생일카페 카드 (HH01). */
public record HomeBirthdayCafeResponse(
    UUID id,
    LocalDate startDate,
    LocalDate endDate,
    Map<String, String> venue,
    String imageKey,
    String linkUrl) {

  public static HomeBirthdayCafeResponse from(BirthdayCafe cafe) {
    return new HomeBirthdayCafeResponse(
        cafe.getId(),
        cafe.getStartDate(),
        cafe.getEndDate(),
        cafe.getVenue(),
        cafe.getImageKey(),
        cafe.getLinkUrl());
  }
}
