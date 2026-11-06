package com.seoulaim.domain.inquiry.dto;

/**
 * 회원별 문의·환불 이력 수 (어드민 화면용). API 응답으로 직접 쓰지 않는다.
 *
 * @param refundedCount 승인된 환불 수 (refund_status REFUNDED)
 * @param rejectedRefundCount 거절된 환불 수 (refund_status REJECTED)
 * @param pendingInquiryCount 답변 전(RECEIVED) 문의 수. 처리 대기 환불 요청 포함
 */
public record UserInquiryStats(
    long refundedCount, long rejectedRefundCount, long pendingInquiryCount) {

  public static final UserInquiryStats EMPTY = new UserInquiryStats(0, 0, 0);
}
