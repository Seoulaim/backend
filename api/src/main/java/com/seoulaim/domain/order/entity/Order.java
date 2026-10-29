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

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

  @Column(name = "order_number", nullable = false, unique = true, length = 30)
  private String orderNumber;

  // TODO(#TBD): users 도메인 반영 후 FK 추가
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private OrderStatus status;

  @Column(name = "total_product_amount", nullable = false)
  private long totalProductAmount;

  @Column(name = "shipping_fee", nullable = false)
  private long shippingFee;

  @Column(name = "final_amount", nullable = false)
  private long finalAmount;

  @Column(name = "recipient_name", nullable = false, length = 100)
  private String recipientName;

  @Column(name = "address_line1", nullable = false, length = 500)
  private String addressLine1;

  @Column(name = "address_line2", length = 500)
  private String addressLine2;

  @Column(name = "postal_code", length = 20)
  private String postalCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "contact_type", length = 20)
  private ContactType contactType;

  @Column(name = "contact_value")
  private String contactValue;

  @Column(name = "ordered_at", nullable = false)
  private Instant orderedAt;

  public static Order createPending(
      String orderNumber,
      UUID userId,
      long totalProductAmount,
      long shippingFee,
      String recipientName,
      String addressLine1,
      String addressLine2,
      String postalCode,
      ContactType contactType,
      String contactValue,
      Instant orderedAt) {
    Order order = new Order();
    order.orderNumber = orderNumber;
    order.userId = userId;
    order.status = OrderStatus.PENDING;
    order.totalProductAmount = totalProductAmount;
    order.shippingFee = shippingFee;
    order.finalAmount = totalProductAmount + shippingFee;
    order.recipientName = recipientName;
    order.addressLine1 = addressLine1;
    order.addressLine2 = addressLine2;
    order.postalCode = postalCode;
    order.contactType = contactType;
    order.contactValue = contactValue;
    order.orderedAt = orderedAt;
    return order;
  }

  public void markPaid() {
    this.status = OrderStatus.PAID;
  }

  public void markFailed() {
    this.status = OrderStatus.FAILED;
  }

  public boolean isPaid() {
    return status == OrderStatus.PAID;
  }

  public boolean isPending() {
    return status == OrderStatus.PENDING;
  }
}
