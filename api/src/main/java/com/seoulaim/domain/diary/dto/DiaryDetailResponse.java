package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.diary.entity.Diary;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DiaryDetailResponse(
    UUID diaryId,
    UUID orderItemId,
    int pageCount,
    Map<String, Object> content,
    Instant submittedAt) {

  public static DiaryDetailResponse from(Diary diary) {
    return new DiaryDetailResponse(
        diary.getId(),
        diary.getOrderItemId(),
        diary.getPageCount(),
        diary.getContent(),
        diary.getSubmittedAt());
  }
}
