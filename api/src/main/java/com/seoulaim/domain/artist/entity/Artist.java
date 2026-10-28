package com.seoulaim.domain.artist.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 그룹 멤버(아티스트) - bias 선택 단위, 장소 연결 단위 - 앱 회원(member)과 구분하려고 artist 로 명명 - 솔로도 1인 그룹에 소속 (상품이 그룹을 필수로
 * 참조하기 때문)
 */
@Getter
@Entity
@Table(
    name = "artists",
    indexes =
        @Index(name = "idx_artists_group_order", columnList = "artist_group_id, display_order"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Artist extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "artist_group_id", nullable = false)
  private ArtistGroup artistGroup;

  @Column(name = "name_en", nullable = false, length = 100)
  private String nameEn;

  @Column(name = "name_ko", nullable = false, length = 100)
  private String nameKo;

  @Column(name = "name_ja", length = 100)
  private String nameJa;

  @Column(name = "name_zh", length = 100)
  private String nameZh;

  @Column(name = "image_key", length = 512)
  private String imageKey;

  // 팬 캘린더 생일 일정 생성에 사용 (별도 등록 불필요)
  @Column(name = "birth_date")
  private LocalDate birthDate;

  // 그룹 내 노출 순서
  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  public static Artist create(
      ArtistGroup artistGroup,
      String nameEn,
      String nameKo,
      String nameJa,
      String nameZh,
      String imageKey,
      LocalDate birthDate,
      int displayOrder) {
    Artist artist = new Artist();
    artist.artistGroup = artistGroup;
    artist.nameEn = nameEn;
    artist.nameKo = nameKo;
    artist.nameJa = nameJa;
    artist.nameZh = nameZh;
    artist.imageKey = imageKey;
    artist.birthDate = birthDate;
    artist.displayOrder = displayOrder;
    return artist;
  }
}
