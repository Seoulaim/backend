package com.seoulaim.global.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** local 전용. 실제 발송 없이 메일 내용을 로그로 출력 */
@Slf4j
@Component
@Profile("local")
public class LoggingEmailSender implements EmailSender {

  @Override
  public void send(String to, String subject, String text) {
    log.info("[메일 발송 생략] to={}, subject={}\n{}", to, subject, text);
  }
}
