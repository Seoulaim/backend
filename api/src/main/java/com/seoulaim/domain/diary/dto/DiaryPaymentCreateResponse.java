package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.diary.entity.DiaryPaymentMethod;
import com.seoulaim.domain.order.gateway.PaymentCurrency;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * @param orderId diary_payments.id. 승인 API 경로에 쓴다
 * @param orderNumber 토스 SDK 의 orderId 로 넘긴다
 * @param finalAmount 결제 통화 단위 금액 (= amount)
 * @param tossClientKey 하위 호환. 결제수단과 관계없이 원화 MID 클라이언트 키
 * @param amount 결제 통화 단위 금액 (KRW 3000, USD 2.99). 토스 SDK amount.value
 * @param clientKey 결제수단의 상점(MID) 클라이언트 키. CARD 는 원화 MID, PAYPAL 은 외화 MID(결제위젯 키)
 * @param variantKey 결제위젯 variantKey. PAYPAL 이면 설정값(기본 "PAYPAL"), CARD 면 null
 * @param customerKey 토스 customerKey (= 회원 id 문자열)
 */
public record DiaryPaymentCreateResponse(
    UUID orderId,
    String orderNumber,
    BigDecimal finalAmount,
    String orderName,
    String tossClientKey,
    DiaryPaymentMethod method,
    PaymentCurrency currency,
    BigDecimal amount,
    String clientKey,
    String variantKey,
    String customerKey) {}
