package com.seoulaim.domain.diary.dto;

import java.util.UUID;

/**
 * @param orderId diary_payments.id. 승인 API 경로에 쓴다
 * @param orderNumber 토스 SDK 의 orderId 로 넘긴다
 */
public record DiaryPaymentCreateResponse(
    UUID orderId, String orderNumber, long finalAmount, String orderName, String tossClientKey) {}
