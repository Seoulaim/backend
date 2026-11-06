package com.seoulaim.domain.user.dto;

import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.entity.UserStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * 어드민 화면용 계정 정보. 다른 도메인이 User 엔티티 대신 쓴다. API 응답으로 직접 쓰지 않는다.
 *
 * @param email 탈퇴 회원은 null
 * @param joinedAt 가입 시각 (users.created_at)
 */
public record UserAccountInfo(
    UUID id,
    String email,
    String nickname,
    String locale,
    UserStatus status,
    Instant joinedAt,
    Instant withdrawnAt) {

  public static UserAccountInfo from(User user) {
    return new UserAccountInfo(
        user.getId(),
        user.getEmail(),
        user.getNickname(),
        user.getLocale(),
        user.getStatus(),
        user.getCreatedAt(),
        user.getWithdrawnAt());
  }

  public boolean withdrawn() {
    return status == UserStatus.WITHDRAWN;
  }
}
