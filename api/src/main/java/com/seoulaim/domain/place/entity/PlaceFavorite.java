package com.seoulaim.domain.place.entity;

import com.seoulaim.domain.user.entity.User;
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

/** 장소 즐겨찾기 - 해제 시 row 삭제 - 즐겨찾기 목록은 최근 추가순 → (user_id, created_at) 인덱스 */
@Getter
@Entity
@Table(
    name = "place_favorites",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_place_favorites_user_place",
            columnNames = {"user_id", "place_id"}),
    indexes = @Index(name = "idx_place_favorites_user_created", columnList = "user_id, created_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceFavorite extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "place_id", nullable = false)
  private Place place;

  public static PlaceFavorite create(User user, Place place) {
    PlaceFavorite favorite = new PlaceFavorite();
    favorite.user = user;
    favorite.place = place;
    return favorite;
  }
}
