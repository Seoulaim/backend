package com.seoulaim.domain.inquiry.service;

import com.seoulaim.global.mail.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 환불 승인이 커밋된 뒤 메일을 보낸다. 돈은 이미 돌려줬으므로 실패는 로그만 남긴다. */
@Slf4j
@Component
@RequiredArgsConstructor
class RefundMailListener {

  private final EmailSender emailSender;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void onRefundApproved(RefundApprovedEvent event) {
    try {
      emailSender.send(event.email(), event.subject(), event.text());
    } catch (RuntimeException exception) {
      log.error("환불 완료 메일 발송 실패 to={}", event.email(), exception);
    }
  }
}
