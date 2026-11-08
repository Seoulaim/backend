package com.seoulaim.domain.diary.entity;

import com.seoulaim.domain.order.gateway.PaymentCurrency;
import com.seoulaim.domain.order.gateway.PaymentMerchant;
import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 다이어리 1건 결제. 토스 orderId 로는 orderNumber 를 쓴다.
 *
 * <p>결제수단(method)마다 토스 상점(merchant)·통화(currency)가 정해진다. amount 는 통화 단위 금액(원, 달러)이다.
 */
@Getter
@Entity
@Table(name = "diary_payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryPayment extends BaseEntity {

  /** 결제 후 이 기간 안이면 환불 가능 */
  public static final Duration REFUND_PERIOD = Duration.ofDays(7);

  static final String SUPERSEDED_CODE = "SUPERSEDED";
  private static final int FAILURE_CODE_MAX_LENGTH = 100;
  private static final int FAILURE_MESSAGE_MAX_LENGTH = 500;
  private static final int METHOD_DETAIL_MAX_LENGTH = 50;

  @Column(name = "diary_id", nullable = false)
  private UUID diaryId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "order_number", nullable = false, unique = true, length = 64)
  private String orderNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "method", nullable = false, length = 10)
  private DiaryPaymentMethod method;

  @Enumerated(EnumType.STRING)
  @Column(name = "merchant", nullable = false, length = 10)
  private PaymentMerchant merchant;

  @Enumerated(EnumType.STRING)
  @Column(name = "currency", nullable = false, length = 3)
  private PaymentCurrency currency;

  // 통화 단위 금액. 조회는 getAmount() (통화 소수 자릿수로 맞춘 값)
  @Getter(AccessLevel.NONE)
  @Column(name = "amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private DiaryPaymentStatus status;

  @Column(name = "refund_consent_at", nullable = false)
  private Instant refundConsentAt;

  @Column(name = "toss_payment_key", length = 200)
  private String tossPaymentKey;

  @Column(name = "paid_at")
  private Instant paidAt;

  // 토스 승인 응답의 결제수단 (예: 카드, 간편결제(토스페이)). 응답에 없거나 #57 이전 결제면 null
  @Column(name = "method_detail", length = METHOD_DETAIL_MAX_LENGTH)
  private String methodDetail;

  // 결제 후 처음 편집한 시각. 환불 정책(편집 전 전액 환불) 판단용
  @Column(name = "edited_at")
  private Instant editedAt;

  @Column(name = "refunded_at")
  private Instant refundedAt;

  // 토스 결제 취소 거래 키 (cancels[].transactionKey)
  @Column(name = "toss_cancel_key", length = 200)
  private String tossCancelKey;

  @Column(name = "failure_code", length = FAILURE_CODE_MAX_LENGTH)
  private String failureCode;

  @Column(name = "failure_message", length = FAILURE_MESSAGE_MAX_LENGTH)
  private String failureMessage;

  /** 원화 카드 결제 대기 건. */
  public static DiaryPayment createPending(
      UUID diaryId, UUID userId, String orderNumber, long amount, Instant refundConsentAt) {
    return createPending(
        diaryId,
        userId,
        orderNumber,
        DiaryPaymentMethod.CARD,
        BigDecimal.valueOf(amount),
        refundConsentAt);
  }

  /**
   * @param amount 결제수단 통화 단위 금액. 통화 소수 자릿수를 넘으면 ArithmeticException
   */
  public static DiaryPayment createPending(
      UUID diaryId,
      UUID userId,
      String orderNumber,
      DiaryPaymentMethod method,
      BigDecimal amount,
      Instant refundConsentAt) {
    DiaryPayment payment = new DiaryPayment();
    payment.diaryId = diaryId;
    payment.userId = userId;
    payment.orderNumber = orderNumber;
    payment.method = method;
    payment.merchant = method.merchant();
    payment.currency = method.currency();
    payment.amount = method.currency().normalize(amount);
    payment.status = DiaryPaymentStatus.PENDING;
    payment.refundConsentAt = refundConsentAt;
    return payment;
  }

  public void markPaid(String tossPaymentKey, Instant paidAt) {
    markPaid(tossPaymentKey, paidAt, null);
  }

  /**
   * @param methodDetail 토스 승인 응답의 결제수단. 없으면 null
   */
  public void markPaid(String tossPaymentKey, Instant paidAt, String methodDetail) {
    this.status = DiaryPaymentStatus.PAID;
    this.tossPaymentKey = tossPaymentKey;
    this.paidAt = paidAt;
    this.methodDetail = truncate(methodDetail, METHOD_DETAIL_MAX_LENGTH);
  }

  /** 통화 소수 자릿수로 맞춘 금액 (KRW 3000, USD 2.99). DB 는 numeric(12,2) 라 KRW 도 3000.00 으로 읽힌다. */
  public BigDecimal getAmount() {
    return currency.normalize(amount);
  }

  public void markFailed(String tossPaymentKey, String code, String message) {
    this.status = DiaryPaymentStatus.FAILED;
    this.tossPaymentKey = tossPaymentKey;
    this.failureCode = truncate(code, FAILURE_CODE_MAX_LENGTH);
    this.failureMessage = truncate(message, FAILURE_MESSAGE_MAX_LENGTH);
  }

  /** 토스 결제 취소 성공. cancelKey 는 응답에 없으면 null. */
  public void markRefunded(String tossCancelKey, Instant refundedAt) {
    this.status = DiaryPaymentStatus.REFUNDED;
    this.tossCancelKey = tossCancelKey;
    this.refundedAt = refundedAt;
  }

  /** 같은 다이어리에 새 결제를 만들면 기존 PENDING 결제는 실패 처리한다. */
  public void supersede() {
    markFailed(null, SUPERSEDED_CODE, "새 결제 요청으로 대체됨");
  }

  /** 결제 후 첫 편집만 기록한다. */
  public void recordEdited(Instant now) {
    if (editedAt == null) {
      this.editedAt = now;
    }
  }

  public boolean isPending() {
    return status == DiaryPaymentStatus.PENDING;
  }

  public boolean isPaid() {
    return status == DiaryPaymentStatus.PAID;
  }

  /** PAID 이고 결제 후 7일 이내이며 다이어리가 아직 완성(FINALIZED)되지 않았으면 환불 가능. */
  public boolean isRefundable(Instant now, boolean diaryFinalized) {
    return isPaid()
        && paidAt != null
        && !now.isAfter(paidAt.plus(REFUND_PERIOD))
        && !diaryFinalized;
  }

  /** 환불 정책(제3조 1항·제4조) 안인지: 환불 가능하고 결제 후 편집 이력이 없다. 환불 문의 접수 시점의 판정으로도 쓴다. */
  public boolean isWithinRefundPolicy(Instant now, boolean diaryFinalized) {
    return isRefundable(now, diaryFinalized) && editedAt == null;
  }

  private static String truncate(String value, int maxLength) {
    return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
  }
}
