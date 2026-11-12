package com.seoulaim.domain.inquiry.admin.dto;

import com.seoulaim.domain.inquiry.entity.InquiryCategory;
import java.time.Instant;
import java.util.UUID;

/**
 * 대시보드 답변 대기 문의 1건 (어드민 mock dashboard.js pendingInquiries 와 같은 필드).
 *
 * @param category 소문자
 * @param nickname 작성자 닉네임. 회원을 찾을 수 없으면 null
 */
public record AdminPendingInquiryResponse(
    UUID id, InquiryCategory category, String title, String nickname, Instant createdAt) {}
