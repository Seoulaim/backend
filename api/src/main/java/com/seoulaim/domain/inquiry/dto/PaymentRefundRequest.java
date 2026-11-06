package com.seoulaim.domain.inquiry.dto;

import java.util.UUID;

/**
 * 결제에 걸린 환불 요청(가장 최근 REFUND 문의). API 응답으로 직접 쓰지 않는다.
 *
 * @param status 소문자 (requested | refunded | rejected)
 */
public record PaymentRefundRequest(UUID inquiryId, String status) {

  public boolean requested() {
    return "requested".equals(status);
  }
}
