package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.diary.entity.Diary;
import java.time.Instant;
import java.util.UUID;

/**
 * 어드민 회원 상세의 다이어리 1개. 삭제·미결제 다이어리도 포함한다. API 응답으로 직접 쓰지 않는다.
 *
 * @param appUnlocked 앱에서 결제 없이 무료 범위를 넘겨 씀 (paid 와 별개)
 * @param finalized 완성(FINALIZED) 여부
 * @param finalizedAt 완성 시각 (completed_at)
 */
public record UserDiaryInfo(
    UUID id,
    String name,
    boolean free,
    boolean paid,
    boolean appUnlocked,
    boolean finalized,
    Instant createdAt,
    Instant lastSavedAt,
    int saveCount,
    Instant finalizedAt,
    long downloadCount,
    boolean deleted) {

  public static UserDiaryInfo of(Diary diary, long downloadCount) {
    return new UserDiaryInfo(
        diary.getId(),
        diary.getName(),
        diary.isFree(),
        diary.isPaid(),
        diary.isAppUnlocked(),
        diary.isFinalized(),
        diary.getCreatedAt(),
        diary.getLastSavedAt(),
        diary.getSaveCount(),
        diary.getCompletedAt(),
        downloadCount,
        diary.isDeleted());
  }
}
