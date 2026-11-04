package com.seoulaim.domain.content.admin.dto;

import com.seoulaim.domain.content.entity.BirthdayCafe;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record AdminBirthdayCafeResponse(
    UUID id,
    UUID artistId,
    Map<String, String> venue,
    LocalDate startDate,
    LocalDate endDate,
    String imageKey,
    String linkUrl,
    int displayOrder,
    Instant createdAt) {

  public static AdminBirthdayCafeResponse from(BirthdayCafe cafe) {
    return new AdminBirthdayCafeResponse(
        cafe.getId(),
        cafe.getArtistId(),
        cafe.getVenue(),
        cafe.getStartDate(),
        cafe.getEndDate(),
        cafe.getImageKey(),
        cafe.getLinkUrl(),
        cafe.getDisplayOrder(),
        cafe.getCreatedAt());
  }
}
