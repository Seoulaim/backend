package com.seoulaim.domain.inquiry.service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

/**
 * 환불 결과 안내 메일 문구. 문의를 쓴 앱 언어(en|ko|zh|ja)로 보내고, 없는 언어면 영어.
 *
 * <p>답변 메일과 같은 제목 형식 "[Seoulaim] Re: {문의 제목}" 을 쓴다.
 */
final class RefundMails {

  record Mail(String subject, String text) {}

  private record Texts(
      String approved,
      String orderLabel,
      String amountLabel,
      String firstDiary,
      String otherDiary,
      String cardNotice,
      String rejected,
      String reasonLabel,
      String contact) {}

  private static final String DEFAULT_LANGUAGE = "en";
  private static final String SIGNATURE = "Seoulaim";

  private static final Map<String, Texts> TEXTS =
      Map.of(
          "en",
          new Texts(
              "Your refund request has been approved and the payment was cancelled.",
              "Order",
              "Amount",
              "As this was your first diary, pages 3 and later are locked again."
                  + " What you made there is kept and comes back if you pay again.",
              "The diary and its photos have been deleted.",
              "Depending on your card company, it may take a few business days to see the refund.",
              "We're sorry, but we couldn't approve your refund request.",
              "Reason",
              "If you have any questions, please contact us through the Help Center."),
          "ko",
          new Texts(
              "환불 요청이 승인되어 결제가 취소되었어요.",
              "주문번호",
              "금액",
              "첫 다이어리라서 3쪽부터 다시 잠겨요. 꾸민 내용은 남아 있고, 다시 결제하면 그대로 볼 수 있어요.",
              "다이어리와 등록한 사진이 삭제되었어요.",
              "카드사에 따라 실제 환불 반영까지 영업일 기준 며칠이 더 걸릴 수 있어요.",
              "죄송하지만 환불 요청을 승인하지 못했어요.",
              "사유",
              "궁금한 점은 Help Center 로 문의해 주세요."),
          "ja",
          new Texts(
              "返金リクエストが承認され、決済がキャンセルされました。",
              "注文番号",
              "金額",
              "最初のダイアリーのため、3ページ目以降は再びロックされます。作成した内容は残り、再度お支払いいただくとそのまま表示されます。",
              "ダイアリーと登録した写真は削除されました。",
              "カード会社によっては、返金の反映まで数営業日かかる場合があります。",
              "申し訳ありませんが、返金リクエストを承認できませんでした。",
              "理由",
              "ご不明な点はHelp Centerからお問い合わせください。"),
          "zh",
          new Texts(
              "您的退款申请已获批准，付款已取消。",
              "订单号",
              "金额",
              "由于这是您的第一本日记，第3页及之后的页面将重新锁定。已制作的内容会保留，再次付款后即可查看。",
              "日记及已上传的照片已被删除。",
              "根据发卡机构的不同，退款可能需要几个工作日才能到账。",
              "很抱歉，我们无法批准您的退款申请。",
              "原因",
              "如有疑问，请通过 Help Center 联系我们。"));

  private RefundMails() {}

  static Mail approved(
      String language, String title, String orderNumber, BigDecimal amount, boolean firstDiary) {
    Texts texts = texts(language);
    String text =
        String.join(
            "\n\n",
            texts.approved(),
            "- %s: %s\n- %s: KRW %s"
                .formatted(
                    texts.orderLabel(),
                    orderNumber,
                    texts.amountLabel(),
                    String.format(Locale.ROOT, "%,d", amount.longValueExact())),
            firstDiary ? texts.firstDiary() : texts.otherDiary(),
            texts.cardNotice(),
            SIGNATURE);
    return new Mail(subject(title), text);
  }

  static Mail rejected(String language, String title, String reason) {
    Texts texts = texts(language);
    String text =
        String.join(
            "\n\n",
            texts.rejected(),
            texts.reasonLabel() + ": " + reason,
            texts.contact(),
            SIGNATURE);
    return new Mail(subject(title), text);
  }

  private static String subject(String title) {
    return AdminInquiryService.ANSWER_SUBJECT_PREFIX + title;
  }

  private static Texts texts(String language) {
    return TEXTS.getOrDefault(language, TEXTS.get(DEFAULT_LANGUAGE));
  }
}
