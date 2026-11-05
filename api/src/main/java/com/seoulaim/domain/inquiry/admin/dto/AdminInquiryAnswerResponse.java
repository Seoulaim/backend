package com.seoulaim.domain.inquiry.admin.dto;

import com.seoulaim.domain.inquiry.entity.InquiryAnswer;
import java.time.Instant;
import java.util.UUID;

/**
 * @param adminName 운영자 계정이 없어 항상 "운영자"
 */
public record AdminInquiryAnswerResponse(UUID id, String body, Instant sentAt, String adminName) {

  public static AdminInquiryAnswerResponse of(InquiryAnswer answer, String adminName) {
    return new AdminInquiryAnswerResponse(
        answer.getId(), answer.getBody(), answer.getSentAt(), adminName);
  }
}
