package com.seoulaim.domain.diary.service;

import com.seoulaim.domain.user.dto.UserAccountInfo;
import com.seoulaim.domain.user.service.UserService;
import com.seoulaim.global.mail.EmailSender;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 결제 승인이 커밋된 뒤 결제 완료 메일을 보낸다. 결제는 이미 끝났으므로 회원 조회·메일 발송 실패는 로그만 남긴다 (API 응답에 영향 없음).
 *
 * <p>회원 정보는 UserService 경유 (user 저장소를 직접 쓰지 않는다). 탈퇴 등으로 이메일이 없으면 보내지 않는다.
 */
@Slf4j
@Component
class PaymentPaidMailListener {

  private final UserService userService;
  private final EmailSender emailSender;
  private final String webBaseUrl;

  PaymentPaidMailListener(
      UserService userService,
      EmailSender emailSender,
      @Value("${app.web.base-url:https://seoulaim.com}") String webBaseUrl) {
    this.userService = userService;
    this.emailSender = emailSender;
    this.webBaseUrl = webBaseUrl;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void onPaymentPaid(DiaryPaymentPaidEvent event) {
    try {
      Optional<UserAccountInfo> account =
          userService.findAccounts(List.of(event.userId())).stream().findFirst();
      String email = account.map(UserAccountInfo::email).orElse(null);
      if (email == null || email.isBlank()) {
        log.warn(
            "결제 완료 메일 생략: 회원 이메일 없음 paymentId={} userId={}", event.paymentId(), event.userId());
        return;
      }
      PaymentPaidMails.Mail mail =
          PaymentPaidMails.paid(
              account.map(UserAccountInfo::locale).orElse(null), event.content(), webBaseUrl);
      emailSender.send(email, mail.subject(), mail.text(), mail.html());
    } catch (RuntimeException exception) {
      log.error("결제 완료 메일 발송 실패 paymentId={}", event.paymentId(), exception);
    }
  }
}
