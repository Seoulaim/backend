package com.seoulaim.domain.goods.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "product_categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductCategory extends BaseEntity {

  @Column(name = "name", nullable = false, length = 50)
  private String name;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  public static ProductCategory create(String name, int displayOrder) {
    ProductCategory category = new ProductCategory();
    category.name = name;
    category.displayOrder = displayOrder;
    return category;
  }

  public void update(String name, int displayOrder) {
    this.name = name;
    this.displayOrder = displayOrder;
  }
}
