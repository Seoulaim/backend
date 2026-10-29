package com.seoulaim.domain.diary.admin.dto;

import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.order.dto.OrderItemInfo;
import java.time.Instant;
import java.util.UUID;

public record AdminDiarySummaryResponse(
    UUID diaryId, UUID userId, String orderNumber, String productName, Instant submittedAt) {

  public static AdminDiarySummaryResponse of(Diary diary, OrderItemInfo item) {
    return new AdminDiarySummaryResponse(
        diary.getId(),
        diary.getUserId(),
        item.orderNumber(),
        item.productName(),
        diary.getSubmittedAt());
  }
}
