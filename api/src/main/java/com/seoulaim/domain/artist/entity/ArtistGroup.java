package com.seoulaim.domain.artist.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 아이돌 그룹 - bias 선택 화면의 그룹 목록 - 상품·프로모션·장소가 그룹 단위로 참조 - 이름은 지원 언어별로 저장 - "group" 은 PostgreSQL 예약어 →
 * 테이블명 artist_groups
 */
@Getter
@Entity
@Table(
    name = "artist_groups",
    indexes = @Index(name = "idx_artist_groups_display_order", columnList = "display_order"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtistGroup extends BaseEntity {

  // 앱 기본 표기
  @Column(name = "name_en", nullable = false, length = 100)
  private String nameEn;

  // 한국어 이름 검색용 → 필수
  @Column(name = "name_ko", nullable = false, length = 100)
  private String nameKo;

  @Column(name = "name_ja", length = 100)
  private String nameJa;

  @Column(name = "name_zh", length = 100)
  private String nameZh;

  @Column(name = "image_key", length = 512)
  private String imageKey;

  @Column(name = "debut_date")
  private LocalDate debutDate;

  // null 이 섞이면 정렬 위치가 DB 설정에 따라 달라짐 → not null
  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  public static ArtistGroup create(
      String nameEn,
      String nameKo,
      String nameJa,
      String nameZh,
      String imageKey,
      LocalDate debutDate,
      int displayOrder) {
    ArtistGroup group = new ArtistGroup();
    group.nameEn = nameEn;
    group.nameKo = nameKo;
    group.nameJa = nameJa;
    group.nameZh = nameZh;
    group.imageKey = imageKey;
    group.debutDate = debutDate;
    group.displayOrder = displayOrder;
    return group;
  }
}
