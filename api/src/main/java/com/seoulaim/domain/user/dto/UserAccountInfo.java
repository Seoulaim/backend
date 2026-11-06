package com.seoulaim.domain.user.dto;

import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.entity.UserStatus;
import com.seoulaim.domain.user.entity.WithdrawnBy;
import java.time.Instant;
import java.util.UUID;

/**
 * 어드민 화면용 계정 정보. 다른 도메인이 User 엔티티 대신 쓴다. API 응답으로 직접 쓰지 않는다.
 *
 * @param email 탈퇴 회원은 null
 * @param joinedAt 가입 시각 (users.created_at)
 * @param withdrawnBy 탈퇴 주체. 탈퇴 전이면 null
 * @param withdrawReason 어드민 강퇴 사유. 없으면 null
 * @param marketingEnabled 마케팅 수신 동의 (users.marketing_enabled)
 * @param marketingUpdatedAt 마케팅 수신 동의를 마지막으로 바꾼 시각. 가입 후 안 바꿨으면 null
 * @param pushEnabled 푸시 수신 (users.push_enabled)
 */
public record UserAccountInfo(
    UUID id,
    String email,
    String nickname,
    String profileImageKey,
    String locale,
    UserStatus status,
    Instant joinedAt,
    Instant withdrawnAt,
    WithdrawnBy withdrawnBy,
    String withdrawReason,
    boolean marketingEnabled,
    Instant marketingUpdatedAt,
    boolean pushEnabled) {

  public static UserAccountInfo from(User user) {
    return new UserAccountInfo(
        user.getId(),
        user.getEmail(),
        user.getNickname(),
        user.getProfileImageKey(),
        user.getLocale(),
        user.getStatus(),
        user.getCreatedAt(),
        user.getWithdrawnAt(),
        user.getWithdrawnBy(),
        user.getWithdrawReason(),
        user.isMarketingEnabled(),
        user.getMarketingUpdatedAt(),
        user.isPushEnabled());
  }

  public boolean withdrawn() {
    return status == UserStatus.WITHDRAWN;
  }
}
