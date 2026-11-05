package com.seoulaim.domain.inquiry.admin.dto;

import java.util.List;

/**
 * @param inquiries 환불 문의 제외, 최신순
 * @param refundCount 환불 관리에서 처리 대기(REQUESTED) 중인 요청 수
 */
public record AdminInquiryListResponse(List<AdminInquiryResponse> inquiries, long refundCount) {}
