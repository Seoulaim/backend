package com.seoulaim.domain.diary.admin.dto;

import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryStatus;
import java.time.Instant;
import java.util.UUID;

public record AdminDiarySummaryResponse(
    UUID id,
    UUID userId,
    String name,
    boolean free,
    boolean paid,
    DiaryStatus status,
    Instant completedAt,
    Instant lastEditedAt,
    Instant createdAt) {

  public static AdminDiarySummaryResponse from(Diary diary) {
    return new AdminDiarySummaryResponse(
        diary.getId(),
        diary.getUserId(),
        diary.getName(),
        diary.isFree(),
        diary.isPaid(),
        diary.getStatus(),
        diary.getCompletedAt(),
        diary.getLastEditedAt(),
        diary.getCreatedAt());
  }
}
