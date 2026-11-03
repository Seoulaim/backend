package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.diary.entity.Diary;
import java.util.List;
import java.util.UUID;

/** 결제 화면. 쿠폰은 MVP 범위 밖이라 항상 빈 배열. */
public record DiaryCheckoutResponse(
    Item item, List<Object> coupons, List<PaymentMethod> paymentMethods) {

  private static final List<PaymentMethod> PAYMENT_METHODS =
      List.of(new PaymentMethod("TOSS", "Card"));

  public static DiaryCheckoutResponse of(Diary diary, long price) {
    return new DiaryCheckoutResponse(
        new Item(diary.getId(), diary.getName(), diary.thumbnailKey(), price),
        List.of(),
        PAYMENT_METHODS);
  }

  public record Item(UUID diaryId, String name, String thumbnailKey, long price) {}

  public record PaymentMethod(String id, String name) {}
}
