package com.seoulaim.domain.diary.admin.dto;

import com.seoulaim.domain.order.gateway.PaymentCurrency;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 어드민 결제 목록 한 페이지.
 *
 * @param summary 검색어·기간 필터를 적용한 전체 합계 (status 필터는 무시)
 */
public record AdminPaymentPageResponse(
    List<AdminPaymentResponse> items, int page, int size, long totalElements, Summary summary) {

  /**
   * @param paidCount 지금 paid 인 결제 수 (모든 통화)
   * @param paidAmount paid 원화(KRW) 결제 금액 합계 (원). 통화가 섞이면 더할 수 없어 원화만 (하위 호환)
   * @param refundedCount refunded 결제 수 (모든 통화)
   * @param refundedAmount refunded 원화(KRW) 결제 금액 합계 (원)
   * @param paidAmounts paid 통화별 금액 합계 { KRW: 6000, USD: 2.99 }. 결제가 없는 통화는 빠진다
   * @param refundedAmounts refunded 통화별 금액 합계
   */
  public record Summary(
      long paidCount,
      long paidAmount,
      long refundedCount,
      long refundedAmount,
      Map<PaymentCurrency, BigDecimal> paidAmounts,
      Map<PaymentCurrency, BigDecimal> refundedAmounts) {}
}
