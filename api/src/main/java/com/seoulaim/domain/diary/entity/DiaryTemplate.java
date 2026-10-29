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
@Table(name = "diary_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryTemplate extends BaseEntity {

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "image_key", nullable = false, length = 512)
  private String imageKey;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  public static DiaryTemplate create(
      String name, String description, String imageKey, int displayOrder, boolean isActive) {
    DiaryTemplate template = new DiaryTemplate();
    template.update(name, description, imageKey, displayOrder, isActive);
    return template;
  }

  public void update(
      String name, String description, String imageKey, int displayOrder, boolean isActive) {
    this.name = name;
    this.description = description;
    this.imageKey = imageKey;
    this.displayOrder = displayOrder;
    this.isActive = isActive;
  }
}
