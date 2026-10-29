package com.seoulaim.domain.place.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장소 ↔ 키워드 다대다 연결 - 추천은 키워드 → 장소 방향으로 조회 → keyword_id 단독 인덱스 추가 - unique 제약 인덱스는 place_id 선두라 이 방향
 * 조회에 미사용
 */
@Getter
@Entity
@Table(
    name = "place_keywords",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_place_keywords_place_keyword",
            columnNames = {"place_id", "keyword_id"}),
    indexes = @Index(name = "idx_place_keywords_keyword_id", columnList = "keyword_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceKeyword extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "place_id", nullable = false)
  private Place place;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "keyword_id", nullable = false)
  private Keyword keyword;

  public static PlaceKeyword create(Place place, Keyword keyword) {
    PlaceKeyword placeKeyword = new PlaceKeyword();
    placeKeyword.place = place;
    placeKeyword.keyword = keyword;
    return placeKeyword;
  }
}
