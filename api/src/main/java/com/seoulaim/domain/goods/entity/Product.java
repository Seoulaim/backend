package com.seoulaim.domain.goods.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private ProductCategory category;

  // TODO(#10): artist 도메인 반영 후 FK 추가
  @Column(name = "artist_group_id", nullable = false)
  private UUID artistGroupId;

  // TODO(#10): artist 도메인 반영 후 FK 추가. null 이면 그룹 상품
  @Column(name = "artist_id")
  private UUID artistId;

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Column(name = "summary", length = 500)
  private String summary;

  @Column(name = "detail_image_key", length = 512)
  private String detailImageKey;

  @Column(name = "price", nullable = false)
  private long price;

  @Column(name = "is_diary", nullable = false)
  private boolean isDiary;

  @Column(name = "sales_count_30d", nullable = false)
  private int salesCount30d;

  public static Product create(
      ProductCategory category,
      UUID artistGroupId,
      UUID artistId,
      String name,
      String summary,
      String detailImageKey,
      long price,
      boolean isDiary) {
    Product product = new Product();
    product.category = category;
    product.artistGroupId = artistGroupId;
    product.artistId = artistId;
    product.name = name;
    product.summary = summary;
    product.detailImageKey = detailImageKey;
    product.price = price;
    product.isDiary = isDiary;
    product.salesCount30d = 0;
    return product;
  }

  public void update(
      ProductCategory category,
      UUID artistGroupId,
      UUID artistId,
      String name,
      String summary,
      String detailImageKey,
      long price,
      boolean isDiary) {
    this.category = category;
    this.artistGroupId = artistGroupId;
    this.artistId = artistId;
    this.name = name;
    this.summary = summary;
    this.detailImageKey = detailImageKey;
    this.price = price;
    this.isDiary = isDiary;
  }
}
