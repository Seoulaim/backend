package com.seoulaim.domain.diary.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 스티커 피커의 카테고리 탭. 스티커와는 diary_sticker_category_map 으로 다대다. */
@Getter
@Entity
@Table(name = "diary_sticker_categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryStickerCategory extends BaseEntity {

  public static final int NAME_MAX_LENGTH = 50;

  @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
  private String name;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  public static DiaryStickerCategory create(String name, int displayOrder) {
    DiaryStickerCategory category = new DiaryStickerCategory();
    category.name = name;
    category.displayOrder = displayOrder;
    return category;
  }

  public void rename(String name) {
    this.name = name;
  }

  public void changeDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }
}
