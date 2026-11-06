package com.seoulaim.domain.user.service;

import com.seoulaim.domain.file.dto.FileDetail;
import com.seoulaim.domain.file.entity.FileDomain;
import com.seoulaim.domain.file.service.FileService;
import com.seoulaim.domain.user.dto.NotificationSettingsResponse;
import com.seoulaim.domain.user.dto.UpdateProfileRequest;
import com.seoulaim.domain.user.dto.UserAccountInfo;
import com.seoulaim.domain.user.dto.UserResponse;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.repository.UserRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.util.LikePattern;
import java.time.Clock;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 조회·생성·프로필 수정
 *
 * <ul>
 *   <li>email 은 소문자 정규화된 값으로 전달
 *   <li>프로필·설정 수정은 탈퇴 회원이면 USER_NOT_FOUND (탈퇴 직후 남은 access token 대비)
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

  private static final int MAX_NICKNAME_LENGTH = 20;

  private final UserRepository userRepository;
  private final FileService fileService;
  private final Clock clock;

  public boolean existsByEmail(String email) {
    return userRepository.existsByEmail(email);
  }

  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  public User getUser(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
  }

  public UserResponse getUserProfile(UUID userId) {
    return UserResponse.from(getUser(userId));
  }

  /** 여러 회원 프로필 (어드민 문의 작성자 표시용). 탈퇴 회원도 포함하며 email 이 null 이다. 없는 id 는 빠진다. */
  public List<UserResponse> findProfiles(Collection<UUID> userIds) {
    if (userIds.isEmpty()) {
      return List.of();
    }
    return userRepository.findAllById(new HashSet<>(userIds)).stream()
        .map(UserResponse::from)
        .toList();
  }

  /** 여러 회원 계정 정보 (어드민 화면용). 탈퇴 회원 포함, 없는 id 는 빠진다. */
  public List<UserAccountInfo> findAccounts(Collection<UUID> userIds) {
    if (userIds.isEmpty()) {
      return List.of();
    }
    return userRepository.findAllById(new HashSet<>(userIds)).stream()
        .map(UserAccountInfo::from)
        .toList();
  }

  /** 이메일·닉네임 부분 일치(대소문자 무시) 회원 id (어드민 검색용). 탈퇴 회원은 이메일이 없어 닉네임으로만 걸린다. */
  public List<UUID> searchUserIds(String keyword) {
    return userRepository.findIdsByEmailOrNicknameLike(LikePattern.containing(keyword));
  }

  @Transactional
  public User createUser(String email, String passwordHash, String nickname) {
    if (userRepository.existsByEmail(email)) {
      throw new BusinessException(ErrorCode.USER_EMAIL_DUPLICATED);
    }
    try {
      // 동시 가입 → uk_users_email_password 위반을 즉시 감지하려고 flush
      return userRepository.saveAndFlush(User.createWithEmail(email, passwordHash, nickname));
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(ErrorCode.USER_EMAIL_DUPLICATED);
    }
  }

  @Transactional
  public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
    boolean removeImage = Boolean.TRUE.equals(request.removeProfileImage());
    if (removeImage && request.profileImageFileId() != null) {
      throw new BusinessException(
          ErrorCode.COMMON_INVALID_REQUEST,
          Map.of("profileImageFileId", "removeProfileImage 와 함께 보낼 수 없습니다"));
    }

    User user = getActiveUser(userId);
    if (request.nickname() != null) {
      user.changeNickname(validateNickname(request.nickname()));
    }
    if (request.profileImageFileId() != null) {
      user.changeProfileImageKey(linkProfileImage(userId, request.profileImageFileId()));
    } else if (removeImage) {
      // TODO(#49): 이전 프로필 이미지 S3 객체 정리. 지금은 key 만 비운다
      user.changeProfileImageKey(null);
    }
    return UserResponse.from(user);
  }

  /**
   * 탈퇴 처리 (이메일·비밀번호 삭제, 상태 WITHDRAWN).
   *
   * @return 이번 호출로 탈퇴했으면 true, 이미 탈퇴한 회원이면 false
   */
  @Transactional
  public boolean withdraw(UUID userId) {
    User user = getUser(userId);
    if (user.isWithdrawn()) {
      return false;
    }
    user.withdraw(clock.instant());
    return true;
  }

  @Transactional
  public void changePassword(UUID userId, String passwordHash) {
    getActiveUser(userId).changePasswordHash(passwordHash);
  }

  public NotificationSettingsResponse getNotificationSettings(UUID userId) {
    return NotificationSettingsResponse.from(getActiveUser(userId));
  }

  @Transactional
  public NotificationSettingsResponse updateNotificationSettings(UUID userId, Boolean marketing) {
    User user = getActiveUser(userId);
    if (marketing != null) {
      user.changeMarketingEnabled(marketing, clock.instant());
    }
    return NotificationSettingsResponse.from(user);
  }

  // language 값(en|ko|zh|ja)은 요청 DTO 에서 검증
  @Transactional
  public void updateLanguage(UUID userId, String language) {
    getActiveUser(userId).changeLocale(language);
  }

  private User getActiveUser(UUID userId) {
    User user = getUser(userId);
    if (user.isWithdrawn()) {
      throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }
    return user;
  }

  // 앞뒤 공백 제거 후 1~20자 (가입 SignupRequest 의 @Size(max = 20) 과 같은 기준)
  private static String validateNickname(String nickname) {
    String trimmed = nickname.trim();
    if (trimmed.isEmpty() || trimmed.length() > MAX_NICKNAME_LENGTH) {
      throw new BusinessException(ErrorCode.USER_INVALID_NICKNAME);
    }
    return trimmed;
  }

  // 없음·남의 파일·PROFILE 이 아닌 파일 → 400 (존재 여부를 구분하지 않음)
  private String linkProfileImage(UUID userId, UUID fileId) {
    FileDetail file =
        fileService
            .findDetail(fileId)
            .filter(found -> userId.equals(found.uploaderId()))
            .filter(found -> found.domain() == FileDomain.PROFILE)
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.COMMON_INVALID_REQUEST,
                        Map.of("profileImageFileId", "본인이 올린 프로필 이미지가 아닙니다")));
    fileService.markLinked(List.of(fileId));
    return file.s3Key();
  }
}
