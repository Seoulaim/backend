package com.seoulaim.domain.diary.service;

import java.util.UUID;

/**
 * 결제 승인(PENDING → PAID)이 커밋된 뒤 결제 완료 메일을 보내기 위한 이벤트. 이미 PAID 였던 중복 승인에는 발행하지 않는다.
 *
 * @param userId 결제 회원. 이메일·언어는 커밋 후 리스너에서 조회한다
 */
record DiaryPaymentPaidEvent(UUID paymentId, UUID userId, PaymentPaidMails.Content content) {}
