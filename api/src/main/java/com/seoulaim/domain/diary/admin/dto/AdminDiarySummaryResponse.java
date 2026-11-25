package com.seoulaim.domain.diary.admin.dto;

import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * @param appUnlocked 앱에서 결제 없이 무료 범위를 넘겨 씀 (paid 와 별개)
 */
public record AdminDiarySummaryResponse(
    UUID id,
    UUID userId,
    String name,
    boolean free,
    boolean paid,
    boolean appUnlocked,
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
        diary.isAppUnlocked(),
        diary.getStatus(),
        diary.getCompletedAt(),
        diary.getLastEditedAt(),
        diary.getCreatedAt());
  }
}
