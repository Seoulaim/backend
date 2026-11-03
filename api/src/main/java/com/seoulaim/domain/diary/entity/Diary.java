package com.seoulaim.domain.diary.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 회원이 앱에서 편집하는 다이어리.
 *
 * <p>spreads / placePhotos 는 웹앱 편집기 데이터로, 서버는 구조를 해석하지 않고 그대로 보관·반환한다.
 */
@Getter
@Entity
@Table(name = "diaries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Diary extends BaseEntity {

  public static final String DEFAULT_NAME = "My Diary";

  // 다른 도메인 엔티티를 참조하지 않으려고 id 만 보관한다. DB 에는 users FK 가 있다
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "name", nullable = false, length = 30)
  private String name;

  @Column(name = "is_free", nullable = false)
  private boolean isFree;

  @Column(name = "paid", nullable = false)
  private boolean paid;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private DiaryStatus status;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "spreads", nullable = false, columnDefinition = "jsonb")
  private List<Object> spreads;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "place_photos", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> placePhotos;

  // 완성 페이지 이미지 S3 key. null 이면 아직 업로드 전
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "page_keys", columnDefinition = "jsonb")
  private List<String> pageKeys;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "last_edited_at")
  private Instant lastEditedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  public static Diary create(UUID userId, boolean isFree) {
    Diary diary = new Diary();
    diary.userId = userId;
    diary.name = DEFAULT_NAME;
    diary.isFree = isFree;
    diary.paid = false;
    diary.status = DiaryStatus.DRAFT;
    diary.spreads = List.of();
    diary.placePhotos = Map.of();
    return diary;
  }

  public void rename(String name) {
    this.name = name;
  }

  public void delete(Instant now) {
    this.deletedAt = now;
  }

  public void saveSpreads(List<Object> spreads, Map<String, Object> placePhotos, Instant now) {
    this.spreads = spreads;
    this.placePhotos = placePhotos;
    this.lastEditedAt = now;
  }

  public void finalizeWith(List<Object> spreads, Map<String, Object> placePhotos, Instant now) {
    saveSpreads(spreads, placePhotos, now);
    this.status = DiaryStatus.FINALIZED;
    this.completedAt = now;
  }

  /** 완성 페이지 key 를 저장하고, 더 쓰지 않는 편집 데이터는 비운다. */
  public void completePages(List<String> pageKeys) {
    this.pageKeys = List.copyOf(pageKeys);
    this.spreads = List.of();
    this.placePhotos = Map.of();
  }

  public void markPaid() {
    this.paid = true;
  }

  public boolean isFinalized() {
    return status == DiaryStatus.FINALIZED;
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }

  public boolean hasPageKeys() {
    return pageKeys != null;
  }

  /** 목록·결제 화면 썸네일. 완성 페이지 첫 장, 없으면 null. */
  public String thumbnailKey() {
    return pageKeys == null || pageKeys.isEmpty() ? null : pageKeys.getFirst();
  }
}
