package com.seoulaim.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.seoulaim.domain.diary.entity.DiaryPaymentMethod;
import com.seoulaim.domain.order.gateway.PaymentCurrency;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PaymentPaidMailsTest {

  private static final Instant PAID_AT = Instant.parse("2026-11-10T05:30:00Z");
  private static final String BASE_URL = "https://seoulaim.com";

  private static PaymentPaidMails.Content card(String diaryName) {
    return new PaymentPaidMails.Content(
        diaryName,
        "DIA-20261110-ABCDE12345",
        DiaryPaymentMethod.CARD,
        PaymentCurrency.KRW,
        new BigDecimal("3000"),
        PAID_AT);
  }

  @ParameterizedTest(name = "{0} → {1}")
  @CsvSource(
      delimiter = '|',
      value = {
        "en|[Seoulaim] Payment confirmed – My Diary",
        "ko|[Seoulaim] 결제 완료 – My Diary",
        "ja|[Seoulaim] お支払い完了 – My Diary",
        "zh|[Seoulaim] 付款成功 – My Diary",
        "fr|[Seoulaim] Payment confirmed – My Diary",
        "KO|[Seoulaim] 결제 완료 – My Diary"
      })
  @DisplayName("제목은 회원 언어로 번역되고 모르는 언어는 영어")
  void paid_언어별_제목(String language, String expected) {
    assertThat(PaymentPaidMails.paid(language, card("My Diary"), BASE_URL).subject())
        .isEqualTo(expected);
  }

  @Test
  @DisplayName("언어가 없으면(null) 영어")
  void paid_언어없음_영어() {
    assertThat(PaymentPaidMails.paid(null, card("My Diary"), BASE_URL).subject())
        .isEqualTo("[Seoulaim] Payment confirmed – My Diary");
  }

  @ParameterizedTest(name = "{0} {1} → {2}")
  @CsvSource(
      delimiter = '|',
      value = {
        "KRW|3000|KRW 3,000",
        "KRW|3000.00|KRW 3,000",
        "KRW|1234567|KRW 1,234,567",
        "USD|2.99|USD 2.99",
        "USD|2.9|USD 2.90",
        "USD|3|USD 3.00",
        "USD|1234.5|USD 1,234.50"
      })
  @DisplayName("금액은 통화별 소수 자릿수와 천 단위 구분으로 표기한다")
  void formatAmount_통화별(PaymentCurrency currency, BigDecimal amount, String expected) {
    assertThat(PaymentPaidMails.formatAmount(currency, amount)).isEqualTo(expected);
  }

  @Test
  @DisplayName("결제 일시는 KST 와 UTC 를 함께 표기한다")
  void formatPaidAt_KST_UTC() {
    assertThat(PaymentPaidMails.formatPaidAt(PAID_AT))
        .isEqualTo("2026-11-10 14:30 (KST) / 2026-11-10 05:30 (UTC)");
  }

  @Test
  @DisplayName("본문(텍스트·HTML)에 다이어리 이름·주문번호·수단·금액·일시·환불 안내·링크가 들어간다")
  void paid_본문_항목() {
    PaymentPaidMails.Content paypal =
        new PaymentPaidMails.Content(
            "Seoul Trip",
            "DIA-20261110-PAYPAL0001",
            DiaryPaymentMethod.PAYPAL,
            PaymentCurrency.USD,
            new BigDecimal("2.99"),
            PAID_AT);

    PaymentPaidMails.Mail mail = PaymentPaidMails.paid("en", paypal, BASE_URL + "/");

    assertThat(mail.text())
        .contains(
            "Seoul Trip",
            "DIA-20261110-PAYPAL0001",
            "PayPal",
            "USD 2.99",
            "2026-11-10 14:30 (KST) / 2026-11-10 05:30 (UTC)",
            "Within 7 days",
            "https://seoulaim.com/mypage/payments",
            "https://seoulaim.com/mypage/inquiry");
    assertThat(mail.html())
        .contains(
            "Seoul Trip",
            "PayPal",
            "USD 2.99",
            "<a href=\"https://seoulaim.com/mypage/payments\">",
            "<a href=\"https://seoulaim.com/mypage/inquiry\">Help Center</a>");
    assertThat(PaymentPaidMails.paid("ko", card("x"), BASE_URL).text())
        .contains("결제 수단: Card", "금액: KRW 3,000");
  }

  @Test
  @DisplayName("HTML 본문에서는 다이어리 이름(회원 입력)을 이스케이프한다")
  void paid_HTML_이스케이프() {
    PaymentPaidMails.Mail mail = PaymentPaidMails.paid("en", card("<b>Hi</b> & \"you\""), BASE_URL);

    assertThat(mail.html()).contains("&lt;b&gt;Hi&lt;/b&gt; &amp; &quot;you&quot;");
    assertThat(mail.html()).doesNotContain("<b>Hi</b>");
    assertThat(mail.text()).contains("<b>Hi</b> & \"you\"");
  }
}
