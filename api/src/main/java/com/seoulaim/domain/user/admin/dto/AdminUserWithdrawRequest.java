package com.seoulaim.domain.user.admin.dto;

/**
 * 어드민 강퇴.
 *
 * @param reason 1~200자 (앞뒤 공백 제거 후)
 */
public record AdminUserWithdrawRequest(String reason) {}
