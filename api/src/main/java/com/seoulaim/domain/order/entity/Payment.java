package com.seoulaim.domain.order.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

  private static final int FAILURE_MESSAGE_MAX_LENGTH = 500;

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Enumerated(EnumType.STRING)
  @Column(name = "pg_provider", nullable = false, length = 30)
  private PgProvider pgProvider;

  @Column(name = "payment_key")
  private String paymentKey;

  @Column(name = "idempotency_key", nullable = false, unique = true)
  private String idempotencyKey;

  @Column(name = "method", length = 30)
  private String method;

  @Column(name = "amount", nullable = false)
  private long amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private PaymentStatus status;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "failure_code", length = 100)
  private String failureCode;

  @Column(name = "failure_message", length = FAILURE_MESSAGE_MAX_LENGTH)
  private String failureMessage;

  // PG 응답 원문. 정산·분쟁 대응용으로 그대로 보관한다
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "raw_response", columnDefinition = "jsonb")
  private String rawResponse;

  public static Payment createReady(
      UUID orderId, PgProvider pgProvider, String idempotencyKey, long amount) {
    Payment payment = new Payment();
    payment.orderId = orderId;
    payment.pgProvider = pgProvider;
    payment.idempotencyKey = idempotencyKey;
    payment.amount = amount;
    payment.status = PaymentStatus.READY;
    return payment;
  }

  public void markDone(String paymentKey, String method, Instant approvedAt, String rawResponse) {
    this.status = PaymentStatus.DONE;
    this.paymentKey = paymentKey;
    this.method = method;
    this.approvedAt = approvedAt;
    this.rawResponse = rawResponse;
  }

  public void markFailed(
      String paymentKey, String failureCode, String failureMessage, String rawResponse) {
    this.status = PaymentStatus.FAILED;
    this.paymentKey = paymentKey;
    this.failureCode = failureCode;
    this.failureMessage = truncate(failureMessage);
    this.rawResponse = rawResponse;
  }

  private static String truncate(String value) {
    if (value == null || value.length() <= FAILURE_MESSAGE_MAX_LENGTH) {
      return value;
    }
    return value.substring(0, FAILURE_MESSAGE_MAX_LENGTH);
  }
}
