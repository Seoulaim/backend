package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.order.dto.OrderItemInfo;
import java.time.Instant;
import java.util.UUID;

public record DiarySummaryResponse(
    UUID diaryId, String orderNumber, String productName, int pageCount, Instant submittedAt) {

  public static DiarySummaryResponse of(Diary diary, OrderItemInfo item) {
    return new DiarySummaryResponse(
        diary.getId(),
        item.orderNumber(),
        item.productName(),
        diary.getPageCount(),
        diary.getSubmittedAt());
  }
}
