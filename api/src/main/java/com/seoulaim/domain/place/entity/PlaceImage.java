package com.seoulaim.domain.place.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 운영자가 등록한 장소 사진 - 장소 상세, 일정 항목 이미지 슬라이드 - 사용자가 일정 항목에 올리는 사진과는 별개 */
@Getter
@Entity
@Table(
    name = "place_images",
    indexes = @Index(name = "idx_place_images_place_order", columnList = "place_id, display_order"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceImage extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "place_id", nullable = false)
  private Place place;

  @Column(name = "image_key", nullable = false, length = 512)
  private String imageKey;

  // 가장 작은 값이 대표 이미지
  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  public static PlaceImage create(Place place, String imageKey, int displayOrder) {
    PlaceImage image = new PlaceImage();
    image.place = place;
    image.imageKey = imageKey;
    image.displayOrder = displayOrder;
    return image;
  }
}
