package com.seoulaim.domain.inquiry.admin.dto;

/**
 * @param reason 거절 사유. 회원에게 메일로 보낸다. 1~1000자
 */
public record AdminRefundRejectRequest(String reason) {}
