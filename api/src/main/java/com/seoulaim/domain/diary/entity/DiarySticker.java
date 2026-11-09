package com.seoulaim.domain.diary.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

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

  // 소속 카테고리 (diary_sticker_category_map). 목록 조회 시 N+1 방지 → 한 번에 로딩
  @BatchSize(size = 100)
  @ElementCollection
  @CollectionTable(
      name = "diary_sticker_category_map",
      joinColumns = @JoinColumn(name = "sticker_id"))
  @Column(name = "category_id", nullable = false)
  private Set<UUID> categoryIds = new LinkedHashSet<>();

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

  /** 카테고리 연결을 통째로 바꾼다. 빈 컬렉션이면 전부 해제. */
  public void replaceCategories(Collection<UUID> categoryIds) {
    this.categoryIds.clear();
    this.categoryIds.addAll(categoryIds);
  }

  public Set<UUID> getCategoryIds() {
    return Collections.unmodifiableSet(categoryIds);
  }
}
