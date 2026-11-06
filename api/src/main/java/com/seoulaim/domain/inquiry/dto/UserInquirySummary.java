package com.seoulaim.domain.inquiry.dto;

import com.seoulaim.domain.inquiry.entity.Inquiry;
import java.time.Instant;
import java.util.UUID;

/**
 * 어드민 회원 상세의 문의 1건. API 응답으로 직접 쓰지 않는다.
 *
 * @param category 소문자 (refund | payment | diary | account | report | other)
 * @param status 소문자 (received | answered)
 * @param refundStatus 환불 문의만. 소문자 (requested | refunded | rejected), 그 외 null
 */
public record UserInquirySummary(
    UUID id, String category, String title, String status, Instant createdAt, String refundStatus) {

  public static UserInquirySummary from(Inquiry inquiry) {
    return new UserInquirySummary(
        inquiry.getId(),
        inquiry.getCategory().apiValue(),
        inquiry.getTitle(),
        inquiry.getStatus().apiValue(),
        inquiry.getCreatedAt(),
        inquiry.getRefundStatus() == null ? null : inquiry.getRefundStatus().apiValue());
  }
}
