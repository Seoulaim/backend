package com.seoulaim.domain.diary.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "diary_stickers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiarySticker extends BaseEntity {

  @Column(name = "name", length = 100)
  private String name;

  @Column(name = "image_key", nullable = false, length = 512)
  private String imageKey;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  public static DiarySticker create(
      String name, String imageKey, int displayOrder, boolean isActive) {
    DiarySticker sticker = new DiarySticker();
    sticker.update(name, imageKey, displayOrder, isActive);
    return sticker;
  }

  public void update(String name, String imageKey, int displayOrder, boolean isActive) {
    this.name = name;
    this.imageKey = imageKey;
    this.displayOrder = displayOrder;
    this.isActive = isActive;
  }
}
