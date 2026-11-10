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

  /**
   * 텍스트와 HTML 을 함께 보낸다 (multipart/alternative). 메일 클라이언트가 HTML 을 못 보이면 텍스트가 보인다.
   *
   * <p>기본 구현은 HTML 없이 텍스트만 보낸다.
   */
  default void send(String to, String subject, String text, String html) {
    send(to, subject, text);
  }
}
