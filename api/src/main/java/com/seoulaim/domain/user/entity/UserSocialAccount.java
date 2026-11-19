package com.seoulaim.domain.user.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 소셜 계정 연결
 *
 * <ul>
 *   <li>providerUserId: 제공자의 사용자 고유 id (구글 sub). 이메일은 바뀔 수 있어 식별에 미사용
 *   <li>한 회원에 제공자별 여러 계정 연결 가능
 *   <li>회원 탈퇴 시 삭제 → 같은 소셜 계정으로 재가입 가능
 * </ul>
 */
@Getter
@Entity
@Table(
    name = "user_social_accounts",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_user_social_accounts_provider_user",
            columnNames = {"provider", "provider_user_id"}),
    indexes = @Index(name = "idx_user_social_accounts_user_id", columnList = "user_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSocialAccount extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 20)
  private SocialProvider provider;

  @Column(name = "provider_user_id", nullable = false, length = 255)
  private String providerUserId;

  public static UserSocialAccount create(
      User user, SocialProvider provider, String providerUserId) {
    UserSocialAccount account = new UserSocialAccount();
    account.user = user;
    account.provider = provider;
    account.providerUserId = providerUserId;
    return account;
  }
}
