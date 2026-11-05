package com.seoulaim.domain.inquiry.admin.dto;

/**
 * @param memo 어드민 전용 메모. null 이나 빈 문자열이면 지운다. 최대 2000자
 */
public record AdminInquiryMemoRequest(String memo) {}
