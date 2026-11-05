package com.seoulaim.domain.diary.dto;

import java.util.UUID;

/**
 * 환불 문의 접수용 결제 판정. API 응답으로 쓰지 않는다.
 *
 * @param withinPolicy 결제 후 7일 이내 && 다이어리 미완성 && 결제 후 편집 없음 (지금 시점)
 */
public record RefundRequestCheck(UUID paymentId, boolean withinPolicy) {}
