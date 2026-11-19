package com.seoulaim.domain.plan.entity;

import com.seoulaim.domain.artist.entity.Artist;
import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 일정 생성 때 고른 아티스트(bias) - 현재 bias 와 무관하게 일정의 bias 태그 기준으로 사용 - displayOrder: 요청 순서 */
@Getter
@Entity
@Table(
    name = "plan_artists",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_plan_artists_plan_artist",
            columnNames = {"plan_id", "artist_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanArtist extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "plan_id", nullable = false)
  private TravelPlan plan;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "artist_id", nullable = false)
  private Artist artist;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  public static PlanArtist create(TravelPlan plan, Artist artist, int displayOrder) {
    PlanArtist planArtist = new PlanArtist();
    planArtist.plan = plan;
    planArtist.artist = artist;
    planArtist.displayOrder = displayOrder;
    return planArtist;
  }
}
