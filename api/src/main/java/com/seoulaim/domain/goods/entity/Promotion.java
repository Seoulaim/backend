package com.seoulaim.domain.goods.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "promotions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Promotion extends BaseEntity {

  // null 이면 상품 없는 배너
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  private Product product;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Column(name = "image_key", length = 512)
  private String imageKey;

  // length = 1 이면 Hibernate 가 char(1) 로 추론하므로 varchar 로 고정
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(name = "importance", nullable = false, length = 1)
  private PromotionImportance importance;

  // TODO(#10): artist 도메인 반영 후 FK 추가
  @Column(name = "artist_group_id")
  private UUID artistGroupId;

  // TODO(#10): artist 도메인 반영 후 FK 추가
  @Column(name = "artist_id")
  private UUID artistId;

  @Column(name = "starts_at")
  private Instant startsAt;

  @Column(name = "ends_at")
  private Instant endsAt;

  @Column(name = "display_order")
  private Integer displayOrder;

  public static Promotion create(
      Product product,
      String title,
      String imageKey,
      PromotionImportance importance,
      UUID artistGroupId,
      UUID artistId,
      Instant startsAt,
      Instant endsAt,
      Integer displayOrder) {
    Promotion promotion = new Promotion();
    promotion.product = product;
    promotion.title = title;
    promotion.imageKey = imageKey;
    promotion.importance = importance;
    promotion.artistGroupId = artistGroupId;
    promotion.artistId = artistId;
    promotion.startsAt = startsAt;
    promotion.endsAt = endsAt;
    promotion.displayOrder = displayOrder;
    return promotion;
  }
}
