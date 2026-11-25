package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.diary.entity.Diary;
import java.time.Instant;
import java.util.UUID;

/**
 * @param appUnlocked 앱에서 무료 범위를 넘겨 써서 결제 없이 편집·완성할 수 있음 (paid 와 별개)
 */
public record DiarySummaryResponse(
    UUID id,
    String name,
    boolean free,
    boolean paid,
    boolean appUnlocked,
    String thumbnailKey,
    Instant lastEditedAt,
    Instant completedAt) {

  public static DiarySummaryResponse from(Diary diary) {
    return new DiarySummaryResponse(
        diary.getId(),
        diary.getName(),
        diary.isFree(),
        diary.isPaid(),
        diary.isAppUnlocked(),
        diary.thumbnailKey(),
        diary.getLastEditedAt(),
        diary.getCompletedAt());
  }
}
