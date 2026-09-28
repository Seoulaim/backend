package com.seoulaim.domain.goods.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "product_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "image_key", nullable = false, length = 512)
  private String imageKey;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  public static ProductImage create(Product product, String imageKey, int displayOrder) {
    ProductImage image = new ProductImage();
    image.product = product;
    image.imageKey = imageKey;
    image.displayOrder = displayOrder;
    return image;
  }
}
