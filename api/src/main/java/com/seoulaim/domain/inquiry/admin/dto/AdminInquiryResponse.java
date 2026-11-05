package com.seoulaim.domain.inquiry.admin.dto;

import com.seoulaim.domain.inquiry.entity.Inquiry;
import com.seoulaim.domain.inquiry.entity.InquiryCategory;
import com.seoulaim.domain.inquiry.entity.InquiryStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 어드민 문의 1건 (환불 문의 제외).
 *
 * @param category 소문자 (payment | diary | account | report | other)
 * @param status 소문자 (received | answered)
 * @param answers 보낸 순서
 */
public record AdminInquiryResponse(
    UUID id,
    InquiryAuthor author,
    String language,
    InquiryCategory category,
    String title,
    String content,
    List<String> photoKeys,
    Instant createdAt,
    InquiryStatus status,
    List<AdminInquiryAnswerResponse> answers,
    String memo) {

  public static AdminInquiryResponse of(
      Inquiry inquiry, InquiryAuthor author, List<AdminInquiryAnswerResponse> answers) {
    return new AdminInquiryResponse(
        inquiry.getId(),
        author,
        inquiry.getLanguage(),
        inquiry.getCategory(),
        inquiry.getTitle(),
        inquiry.getContent(),
        inquiry.getPhotoKeys(),
        inquiry.getCreatedAt(),
        inquiry.getStatus(),
        answers,
        inquiry.getMemo());
  }
}
