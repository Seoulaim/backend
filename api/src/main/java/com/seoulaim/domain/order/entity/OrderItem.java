package com.seoulaim.domain.order.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  // goods 엔티티를 직접 참조하지 않으려고 id 만 보관한다. DB 에는 FK 가 있다
  @Column(name = "product_id", nullable = false)
  private UUID productId;

  @Column(name = "product_name", nullable = false, length = 200)
  private String productName;

  @Column(name = "unit_price", nullable = false)
  private long unitPrice;

  @Column(name = "quantity", nullable = false)
  private int quantity;

  public static OrderItem create(
      UUID orderId, UUID productId, String productName, long unitPrice, int quantity) {
    OrderItem item = new OrderItem();
    item.orderId = orderId;
    item.productId = productId;
    item.productName = productName;
    item.unitPrice = unitPrice;
    item.quantity = quantity;
    return item;
  }

  public long getAmount() {
    return unitPrice * quantity;
  }
}
