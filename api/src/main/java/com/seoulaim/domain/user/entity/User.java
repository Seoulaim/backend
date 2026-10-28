package com.seoulaim.domain.user.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 앱 사용자 - 이메일 가입: email 이 로그인 아이디, 가입 전 이메일 OTP 인증 필수 - 소셜 계정 정보는 별도 테이블 → email, password_hash 는
 * nullable - 이메일 중복 방지는 lower(email) partial unique 인덱스 (마이그레이션 SQL) - refresh token, OTP 는 Redis 에
 * 저장
 */
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

  private static final String DEFAULT_LOCALE = "en";

  // 소문자로 정규화해 저장 (서비스에서 처리)
  @Column(name = "email", length = 255)
  private String email;

  // bcrypt 해시. 소셜 전용 사용자는 null
  @Column(name = "password_hash", length = 100)
  private String passwordHash;

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

  // 이메일 OTP 인증 완료 여부는 호출 전 서비스에서 확인
  public static User createWithEmail(String email, String passwordHash, String nickname) {
    User user = new User();
    user.email = email;
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

  // 로그인 정보·이메일 삭제 → 같은 이메일로 재가입 가능, 개인정보 미보관
  public void withdraw(Instant withdrawnAt) {
    this.email = null;
    this.passwordHash = null;
    this.status = UserStatus.WITHDRAWN;
    this.withdrawnAt = withdrawnAt;
  }

  public void changeNickname(String nickname) {
    this.nickname = nickname;
  }
}
