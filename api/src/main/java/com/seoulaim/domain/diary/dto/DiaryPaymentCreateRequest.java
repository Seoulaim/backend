package com.seoulaim.domain.diary.dto;

import java.util.UUID;

/**
 * @param couponId 쿠폰 미구현. 받기만 하고 무시한다
 * @param refundConsent 환불 규정 동의. true 가 아니면 PAYMENT_CONSENT_REQUIRED
 */
public record DiaryPaymentCreateRequest(UUID couponId, Boolean refundConsent) {}
