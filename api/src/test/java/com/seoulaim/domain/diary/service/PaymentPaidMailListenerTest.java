package com.seoulaim.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.diary.entity.DiaryPaymentMethod;
import com.seoulaim.domain.order.gateway.PaymentCurrency;
import com.seoulaim.domain.user.dto.UserAccountInfo;
import com.seoulaim.domain.user.entity.UserStatus;
import com.seoulaim.domain.user.service.UserService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.mail.EmailSender;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentPaidMailListenerTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final DiaryPaymentPaidEvent EVENT =
      new DiaryPaymentPaidEvent(
          UUID.randomUUID(),
          USER_ID,
          new PaymentPaidMails.Content(
              "My Diary",
              "DIA-20261110-ABCDE12345",
              DiaryPaymentMethod.CARD,
              PaymentCurrency.KRW,
              new BigDecimal("3000"),
              Instant.parse("2026-11-10T05:30:00Z")));

  @Mock private UserService userService;
  @Mock private EmailSender emailSender;

  private PaymentPaidMailListener listener;

  @BeforeEach
  void setUp() {
    listener = new PaymentPaidMailListener(userService, emailSender, "https://seoulaim.com");
  }

  private static UserAccountInfo account(String email, String locale) {
    return new UserAccountInfo(
        USER_ID,
        email,
        "tester",
        null,
        locale,
        UserStatus.ACTIVE,
        Instant.now(),
        null,
        null,
        null,
        false,
        null,
        true);
  }

  @Test
  @DisplayName("회원 언어로 만든 메일을 회원 이메일로 1번 보낸다")
  void onPaymentPaid_회원언어로발송() {
    given(userService.findAccounts(List.of(USER_ID)))
        .willReturn(List.of(account("a@test.com", "ko")));

    listener.onPaymentPaid(EVENT);

    verify(emailSender)
        .send(
            eq("a@test.com"),
            eq("[Seoulaim] 결제 완료 – My Diary"),
            contains("KRW 3,000"),
            contains("<html>"));
  }

  @Test
  @DisplayName("회원 이메일이 없으면(탈퇴·없는 회원) 보내지 않는다")
  void onPaymentPaid_이메일없음_생략() {
    given(userService.findAccounts(List.of(USER_ID))).willReturn(List.of(account(null, "en")));
    listener.onPaymentPaid(EVENT);

    given(userService.findAccounts(List.of(USER_ID))).willReturn(List.of());
    listener.onPaymentPaid(EVENT);

    verify(emailSender, never()).send(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("발송이 실패해도 예외를 밖으로 던지지 않는다 (로그만)")
  void onPaymentPaid_발송실패_예외없음() {
    given(userService.findAccounts(List.of(USER_ID)))
        .willReturn(List.of(account("a@test.com", "en")));
    willThrow(new BusinessException(ErrorCode.AUTH_MAIL_SEND_FAILED))
        .given(emailSender)
        .send(anyString(), anyString(), anyString(), anyString());

    assertThatCode(() -> listener.onPaymentPaid(EVENT)).doesNotThrowAnyException();
  }
}
