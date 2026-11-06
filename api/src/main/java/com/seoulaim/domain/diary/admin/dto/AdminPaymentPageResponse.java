package com.seoulaim.domain.diary.admin.dto;

import java.util.List;

/**
 * 어드민 결제 목록 한 페이지.
 *
 * @param summary 검색어·기간 필터를 적용한 전체 합계 (status 필터는 무시)
 */
public record AdminPaymentPageResponse(
    List<AdminPaymentResponse> items, int page, int size, long totalElements, Summary summary) {

  /**
   * @param paidCount 지금 paid 인 결제 수
   * @param paidAmount paid 결제 금액 합계 (원)
   * @param refundedCount refunded 결제 수
   * @param refundedAmount refunded 결제 금액 합계 (원)
   */
  public record Summary(long paidCount, long paidAmount, long refundedCount, long refundedAmount) {}
}
