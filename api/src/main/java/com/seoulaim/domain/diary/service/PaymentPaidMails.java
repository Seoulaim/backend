package com.seoulaim.domain.diary.service;

import com.seoulaim.domain.diary.entity.DiaryPaymentMethod;
import com.seoulaim.domain.order.gateway.PaymentCurrency;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.web.util.HtmlUtils;

/**
 * 결제 완료 메일 문구. 회원 언어(users.locale: en|ko|ja|zh)로 보내고, 없거나 모르는 언어면 영어.
 *
 * <p>텍스트와 간단한 HTML 을 함께 만든다. 다이어리 이름은 회원 입력이라 HTML 에서는 이스케이프한다.
 */
final class PaymentPaidMails {

  record Mail(String subject, String text, String html) {}

  /**
   * 메일에 넣을 결제 정보.
   *
   * @param amount 통화 소수 자릿수로 맞춘 금액 (KRW 3000, USD 2.99)
   */
  record Content(
      String diaryName,
      String orderNumber,
      DiaryPaymentMethod method,
      PaymentCurrency currency,
      BigDecimal amount,
      Instant paidAt) {}

  private record Texts(
      String subject,
      String intro,
      String diaryLabel,
      String orderLabel,
      String methodLabel,
      String amountLabel,
      String paidAtLabel,
      String refundTitle,
      List<String> refundLines,
      String historyLabel,
      String helpLabel) {}

  static final String PAYMENTS_PATH = "/mypage/payments";
  static final String HELP_CENTER_PATH = "/mypage/inquiry";
  private static final String DEFAULT_LANGUAGE = "en";
  private static final String SIGNATURE = "Seoulaim";
  private static final String SUBJECT_PREFIX = "[Seoulaim] ";
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private static final Map<String, Texts> TEXTS =
      Map.of(
          "en",
          new Texts(
              "Payment confirmed – %s",
              "Thank you! Your payment has been confirmed.",
              "Diary",
              "Order number",
              "Payment method",
              "Amount",
              "Paid at",
              "Refunds",
              List.of(
                  "Within 7 days of payment, you can request a refund in the Help Center if you"
                      + " haven't edited the diary yet.",
                  "Once you start editing after payment, or finish the diary with \"Done\", it"
                      + " can't be refunded.",
                  "After 7 days from payment, refunds aren't available."),
              "Payment history",
              "Help Center"),
          "ko",
          new Texts(
              "결제 완료 – %s",
              "결제가 완료되었어요. 감사합니다!",
              "다이어리",
              "주문번호",
              "결제 수단",
              "금액",
              "결제 일시",
              "환불 안내",
              List.of(
                  "결제 후 7일 이내이고 다이어리를 편집하기 전이라면 Help Center 에서 환불을 요청할 수 있어요.",
                  "결제 후 편집을 시작했거나 \"Done\"으로 완성한 다이어리는 환불되지 않아요.",
                  "결제일로부터 7일이 지나면 환불되지 않아요."),
              "결제 내역",
              "Help Center"),
          "ja",
          new Texts(
              "お支払い完了 – %s",
              "お支払いが完了しました。ありがとうございます！",
              "ダイアリー",
              "注文番号",
              "お支払い方法",
              "金額",
              "お支払い日時",
              "返金について",
              List.of(
                  "お支払いから7日以内で、ダイアリーを編集する前であれば、Help Centerから返金をリクエストできます。",
                  "お支払い後に編集を始めた、または「Done」で完成したダイアリーは返金できません。",
                  "お支払い日から7日を過ぎると返金できません。"),
              "お支払い履歴",
              "Help Center"),
          "zh",
          new Texts(
              "付款成功 – %s",
              "您的付款已完成，感谢您的支持！",
              "日记",
              "订单号",
              "付款方式",
              "金额",
              "付款时间",
              "退款说明",
              List.of(
                  "付款后7天内且尚未编辑日记时，可在 Help Center 申请退款。",
                  "付款后已开始编辑或已点击\"Done\"完成的日记不予退款。",
                  "自付款之日起超过7天不予退款。"),
              "付款记录",
              "Help Center"));

  private PaymentPaidMails() {}

  static Mail paid(String language, Content content, String webBaseUrl) {
    Texts texts = texts(language);
    String subject = SUBJECT_PREFIX + texts.subject().formatted(content.diaryName());
    String amount = formatAmount(content.currency(), content.amount());
    String method = methodName(content.method());
    String paidAt = formatPaidAt(content.paidAt());
    String base = trimTrailingSlash(webBaseUrl);
    String paymentsUrl = base + PAYMENTS_PATH;
    String helpUrl = base + HELP_CENTER_PATH;

    String text =
        String.join(
            "\n\n",
            texts.intro(),
            String.join(
                "\n",
                "- " + texts.diaryLabel() + ": " + content.diaryName(),
                "- " + texts.orderLabel() + ": " + content.orderNumber(),
                "- " + texts.methodLabel() + ": " + method,
                "- " + texts.amountLabel() + ": " + amount,
                "- " + texts.paidAtLabel() + ": " + paidAt),
            texts.refundTitle()
                + "\n"
                + String.join("\n", texts.refundLines().stream().map(line -> "- " + line).toList()),
            texts.historyLabel() + ": " + paymentsUrl + "\n" + texts.helpLabel() + ": " + helpUrl,
            SIGNATURE);

    String html =
        """
        <!doctype html>
        <html><body style="font-family:-apple-system,'Segoe UI',sans-serif;color:#222;line-height:1.6">
        <p>%s</p>
        <table style="border-collapse:collapse">
        %s</table>
        <h3 style="margin-top:24px">%s</h3>
        <ul>
        %s</ul>
        <p><a href="%s">%s</a> · <a href="%s">%s</a></p>
        <p>%s</p>
        </body></html>
        """
            .formatted(
                escape(texts.intro()),
                row(texts.diaryLabel(), content.diaryName())
                    + row(texts.orderLabel(), content.orderNumber())
                    + row(texts.methodLabel(), method)
                    + row(texts.amountLabel(), amount)
                    + row(texts.paidAtLabel(), paidAt),
                escape(texts.refundTitle()),
                String.join(
                    "",
                    texts.refundLines().stream()
                        .map(line -> "<li>" + escape(line) + "</li>\n")
                        .toList()),
                escape(paymentsUrl),
                escape(texts.historyLabel()),
                escape(helpUrl),
                escape(texts.helpLabel()),
                SIGNATURE);
    return new Mail(subject, text, html);
  }

  // KRW 3000 → "KRW 3,000", USD 2.99 → "USD 2.99"
  static String formatAmount(PaymentCurrency currency, BigDecimal amount) {
    int digits = currency.fractionDigits();
    DecimalFormat format =
        new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.ROOT));
    format.setMinimumFractionDigits(digits);
    format.setMaximumFractionDigits(digits);
    return currency.name() + " " + format.format(currency.normalize(amount));
  }

  // "2026-11-10 14:30 (KST) / 2026-11-10 05:30 (UTC)"
  static String formatPaidAt(Instant paidAt) {
    return DATE_TIME.format(paidAt.atZone(KST))
        + " (KST) / "
        + DATE_TIME.format(paidAt.atZone(ZoneOffset.UTC))
        + " (UTC)";
  }

  private static String methodName(DiaryPaymentMethod method) {
    return switch (method) {
      case CARD -> DiaryPaymentService.CARD_NAME;
      case PAYPAL -> DiaryPaymentService.PAYPAL_NAME;
    };
  }

  private static String row(String label, String value) {
    return "<tr><td style=\"padding:2px 16px 2px 0;color:#888\">%s</td><td>%s</td></tr>\n"
        .formatted(escape(label), escape(value));
  }

  private static String escape(String value) {
    return HtmlUtils.htmlEscape(value == null ? "" : value);
  }

  private static String trimTrailingSlash(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  private static Texts texts(String language) {
    return TEXTS.getOrDefault(
        language == null ? DEFAULT_LANGUAGE : language.toLowerCase(Locale.ROOT),
        TEXTS.get(DEFAULT_LANGUAGE));
  }
}
