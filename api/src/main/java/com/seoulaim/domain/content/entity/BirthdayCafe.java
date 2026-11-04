package com.seoulaim.domain.content.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 생일카페. 생일인 멤버(artistId) 의 그룹 팬에게 노출된다. 멤버 삭제 시 DB 에서 함께 삭제(ON DELETE CASCADE). */
@Getter
@Entity
@Table(name = "birthday_cafes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BirthdayCafe extends BaseEntity {

  // artists FK (ON DELETE CASCADE). 다른 도메인 엔티티를 참조하지 않도록 UUID 로 보관
  @Column(name = "artist_id", nullable = false)
  private UUID artistId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "venue", nullable = false, columnDefinition = "jsonb")
  private Map<String, String> venue;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "image_key", length = 512)
  private String imageKey;

  @Column(name = "link_url", length = 1000)
  private String linkUrl;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  public static BirthdayCafe create(
      UUID artistId,
      Map<String, String> venue,
      LocalDate startDate,
      LocalDate endDate,
      String imageKey,
      String linkUrl,
      int displayOrder) {
    BirthdayCafe cafe = new BirthdayCafe();
    cafe.displayOrder = displayOrder;
    cafe.update(artistId, venue, startDate, endDate, imageKey, linkUrl);
    return cafe;
  }

  public void update(
      UUID artistId,
      Map<String, String> venue,
      LocalDate startDate,
      LocalDate endDate,
      String imageKey,
      String linkUrl) {
    this.artistId = artistId;
    this.venue = Map.copyOf(venue);
    this.startDate = startDate;
    this.endDate = endDate;
    this.imageKey = imageKey;
    this.linkUrl = linkUrl;
  }
}
