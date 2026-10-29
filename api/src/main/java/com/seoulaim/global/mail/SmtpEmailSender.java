package com.seoulaim.global.mail;

import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * SMTP 메일 발송
 *
 * <ul>
 *   <li>JavaMailSender 는 spring.mail.host 설정 시에만 생성 → 미설정이어도 앱 기동 가능
 *   <li>미설정 상태로 발송 요청 시 AUTH_MAIL_SEND_FAILED
 * </ul>
 */
@Slf4j
@Component
@Profile("!local")
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

  private final ObjectProvider<JavaMailSender> mailSenderProvider;

  // 비어 있으면 SMTP 계정 기본 발신자 사용
  @Value("${app.mail.from:}")
  private String from;

  @Override
  public void send(String to, String subject, String text) {
    JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
    if (mailSender == null) {
      log.error("JavaMailSender 없음 → spring.mail.host 설정 확인");
      throw new BusinessException(ErrorCode.AUTH_MAIL_SEND_FAILED);
    }

    SimpleMailMessage message = new SimpleMailMessage();
    if (StringUtils.hasText(from)) {
      message.setFrom(from);
    }
    message.setTo(to);
    message.setSubject(subject);
    message.setText(text);

    try {
      mailSender.send(message);
    } catch (MailException exception) {
      log.error("메일 발송 실패 to={}", to, exception);
      throw new BusinessException(ErrorCode.AUTH_MAIL_SEND_FAILED);
    }
  }
}
