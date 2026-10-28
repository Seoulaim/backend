package com.seoulaim.domain.user.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 앱 사용자 - 아이디/비밀번호 가입, 소셜 가입 모두 수용 - 소셜 계정 정보는 별도 테이블 → login_id, password_hash 는 nullable -
 * refresh token 은 Redis 에 저장
 */
@Getter
@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(name = "uk_users_login_id", columnNames = "login_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

  private static final String DEFAULT_LOCALE = "en";

  // 소셜 전용 사용자는 null (필수 여부는 회원가입 서비스에서 검증)
  // PostgreSQL unique 는 NULL 중복 허용 → 소셜 사용자끼리 충돌 없음
  @Column(name = "login_id", length = 30)
  private String loginId;

  // bcrypt 해시
  @Column(name = "password_hash", length = 100)
  private String passwordHash;

  // 소셜 제공자에 따라 미제공 가능
  @Column(name = "email", length = 255)
  private String email;

  // 화면 입력 길이 제한은 Request DTO 에서 검증
  @Column(name = "nickname", nullable = false, length = 50)
  private String nickname;

  @Column(name = "profile_image_key", length = 512)
  private String profileImageKey;

  // en / ko / ja / zh. 푸시 문구 언어 선택에 사용
  @Column(name = "locale", nullable = false, length = 10)
  private String locale;

  @Column(name = "push_enabled", nullable = false)
  private boolean pushEnabled;

  @Column(name = "marketing_enabled", nullable = false)
  private boolean marketingEnabled;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private UserStatus status;

  // 탈퇴 후에도 row 유지 → 일정·즐겨찾기 FK 보존
  @Column(name = "withdrawn_at")
  private Instant withdrawnAt;

  public static User createWithPassword(String loginId, String passwordHash, String nickname) {
    User user = new User();
    user.loginId = loginId;
    user.passwordHash = passwordHash;
    user.nickname = nickname;
    user.locale = DEFAULT_LOCALE;
    user.pushEnabled = true;
    user.marketingEnabled = false;
    user.status = UserStatus.ACTIVE;
    return user;
  }

  public boolean canLogin() {
    return status == UserStatus.ACTIVE;
  }

  // 로그인 정보 삭제 → 같은 아이디로 재가입 가능
  public void withdraw(Instant withdrawnAt) {
    this.loginId = null;
    this.passwordHash = null;
    this.status = UserStatus.WITHDRAWN;
    this.withdrawnAt = withdrawnAt;
  }

  public void changeNickname(String nickname) {
    this.nickname = nickname;
  }
}
