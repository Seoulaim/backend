package com.seoulaim.domain.diary.dto;

import java.util.UUID;

/**
 * @param couponId 쿠폰 미구현. 받기만 하고 무시한다
 * @param refundConsent 환불 규정 동의. true 가 아니면 PAYMENT_CONSENT_REQUIRED
 * @param method CARD | PAYPAL (대소문자 무시). 없으면 CARD. 그 밖의 값은 PAYMENT_INVALID_METHOD
 */
public record DiaryPaymentCreateRequest(UUID couponId, Boolean refundConsent, String method) {

  public DiaryPaymentCreateRequest(UUID couponId, Boolean refundConsent) {
    this(couponId, refundConsent, null);
  }
}
