package com.seoulaim.global.mail;

/**
 * 메일 발송
 *
 * <ul>
 *   <li>local: 로그 출력 (LoggingEmailSender)
 *   <li>그 외: SMTP 발송 (SmtpEmailSender)
 *   <li>실패 시 AUTH_MAIL_SEND_FAILED
 * </ul>
 */
public interface EmailSender {

  void send(String to, String subject, String text);
}
