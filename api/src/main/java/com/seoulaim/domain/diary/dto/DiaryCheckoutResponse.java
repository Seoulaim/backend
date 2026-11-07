package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryPaymentMethod;
import com.seoulaim.domain.order.gateway.PaymentCurrency;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 결제 화면. 쿠폰은 MVP 범위 밖이라 항상 빈 배열.
 *
 * @param paymentMethods 지금 받을 수 있는 결제수단만 (외화 MID 키가 없으면 PAYPAL 제외)
 */
public record DiaryCheckoutResponse(
    Item item, List<Object> coupons, List<PaymentMethod> paymentMethods) {

  /**
   * @param paymentMethods 사용 가능한 결제수단과 그 금액 (화면 노출 순서)
   */
  public static DiaryCheckoutResponse of(
      Diary diary, long price, List<PaymentMethod> paymentMethods) {
    Map<PaymentCurrency, BigDecimal> prices = new EnumMap<>(PaymentCurrency.class);
    paymentMethods.forEach(method -> prices.put(method.currency(), method.amount()));
    return new DiaryCheckoutResponse(
        new Item(diary.getId(), diary.getName(), diary.thumbnailKey(), price, prices),
        List.of(),
        paymentMethods);
  }

  /**
   * @param price 원화 가격 (하위 호환)
   * @param prices 통화별 가격 { KRW: 3000, USD: 2.99 }. 사용 가능한 결제수단의 통화만
   */
  public record Item(
      UUID diaryId,
      String name,
      String thumbnailKey,
      long price,
      Map<PaymentCurrency, BigDecimal> prices) {}

  /**
   * @param id CARD | PAYPAL. 결제 생성 요청의 method 로 그대로 보낸다
   * @param amount 통화 단위 금액 (KRW 3000, USD 2.99)
   */
  public record PaymentMethod(
      DiaryPaymentMethod id, String name, PaymentCurrency currency, BigDecimal amount) {}
}
