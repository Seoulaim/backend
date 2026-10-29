package com.seoulaim.domain.artist.entity;

import com.seoulaim.domain.user.entity.User;
import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자가 고른 bias - 홈 노출, 일정 추천 기본값 - 그룹이 아닌 아티스트 단위로 선택 - 사용자당 최대 개수는 서비스에서 검증 - 노출 순서는 등록
 * 순(created_at)
 */
@Getter
@Entity
@Table(
    name = "user_biases",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_user_biases_user_artist",
            columnNames = {"user_id", "artist_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBias extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "artist_id", nullable = false)
  private Artist artist;

  public static UserBias create(User user, Artist artist) {
    UserBias bias = new UserBias();
    bias.user = user;
    bias.artist = artist;
    return bias;
  }
}
