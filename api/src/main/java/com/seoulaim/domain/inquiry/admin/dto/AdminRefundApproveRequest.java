package com.seoulaim.domain.inquiry.admin.dto;

/**
 * 환불 승인.
 *
 * @param exception 정책 밖 요청이면 필수
 */
public record AdminRefundApproveRequest(PolicyException exception) {

  /**
   * @param reason 어드민 사유 id (duplicate, not-provided, company-error, misrepresented, other 등). 최대
   *     50자
   * @param memo reason=other 면 필수. 최대 2000자
   */
  public record PolicyException(String reason, String memo) {}
}
