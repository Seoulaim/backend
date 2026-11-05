package com.seoulaim.domain.inquiry.service;

/**
 * 환불 승인 커밋 후 "환불 완료" 메일을 보내기 위한 이벤트. 토스 취소가 끝난 뒤라 메일 실패로 롤백하지 않는다.
 *
 * @param email 작성자 이메일 (null 이면 발행하지 않는다)
 */
record RefundApprovedEvent(String email, String subject, String text) {}
