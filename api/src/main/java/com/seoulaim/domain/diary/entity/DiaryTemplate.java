package com.seoulaim.domain.diary.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "diary_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryTemplate extends BaseEntity {

  /** 페이지 비율(세로/가로) 기본값. A4 297/210. DB 컬럼 기본값과 같다. */
  public static final double DEFAULT_ASPECT = 1.4142857;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "image_key", nullable = false, length = 512)
  private String imageKey;

  @Column(name = "overlay_image_key", length = 512)
  private String overlayImageKey;

  @Column(name = "aspect", nullable = false)
  private double aspect;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "frames", nullable = false, columnDefinition = "jsonb")
  private List<TemplateFrame> frames;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  /** 레이아웃 없이 만든다 (오버레이 없음, 기본 비율, 프레임 없음). */
  public static DiaryTemplate create(
      String name, String description, String imageKey, int displayOrder, boolean isActive) {
    return create(
        name, description, imageKey, null, DEFAULT_ASPECT, List.of(), displayOrder, isActive);
  }

  public static DiaryTemplate create(
      String name,
      String description,
      String imageKey,
      String overlayImageKey,
      double aspect,
      List<TemplateFrame> frames,
      int displayOrder,
      boolean isActive) {
    DiaryTemplate template = new DiaryTemplate();
    template.update(
        name, description, imageKey, overlayImageKey, aspect, frames, displayOrder, isActive);
    return template;
  }

  public void update(
      String name,
      String description,
      String imageKey,
      String overlayImageKey,
      double aspect,
      List<TemplateFrame> frames,
      int displayOrder,
      boolean isActive) {
    this.name = name;
    this.description = description;
    this.imageKey = imageKey;
    this.overlayImageKey = overlayImageKey;
    this.aspect = aspect;
    this.frames = List.copyOf(frames);
    this.displayOrder = displayOrder;
    this.isActive = isActive;
  }
}
