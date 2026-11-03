package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.diary.entity.Diary;
import java.time.Instant;
import java.util.UUID;

public record DiarySummaryResponse(
    UUID id,
    String name,
    boolean free,
    boolean paid,
    String thumbnailKey,
    Instant lastEditedAt,
    Instant completedAt) {

  public static DiarySummaryResponse from(Diary diary) {
    return new DiarySummaryResponse(
        diary.getId(),
        diary.getName(),
        diary.isFree(),
        diary.isPaid(),
        diary.thumbnailKey(),
        diary.getLastEditedAt(),
        diary.getCompletedAt());
  }
}
