package com.seoulaim.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.file.dto.FileDetail;
import com.seoulaim.domain.file.entity.FileDomain;
import com.seoulaim.domain.file.entity.FileStatus;
import com.seoulaim.domain.file.service.FileService;
import com.seoulaim.domain.user.dto.NotificationSettingsResponse;
import com.seoulaim.domain.user.dto.UpdateProfileRequest;
import com.seoulaim.domain.user.dto.UserResponse;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.repository.UserRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-04T06:00:00Z");
  private static final UUID USER_ID = UUID.randomUUID();

  @Mock private UserRepository userRepository;
  @Mock private FileService fileService;

  private UserService userService;
  private User user;

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository, fileService, Clock.fixed(NOW, ZoneOffset.UTC));
    user = User.createWithEmail("luna@example.com", "hash", "Luna");
    ReflectionTestUtils.setField(user, "id", USER_ID);
  }

  @Test
  @DisplayName("닉네임은 앞뒤 공백을 제거해 저장한다")
  void updateProfile_닉네임_공백제거후저장() {
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

    UserResponse response =
        userService.updateProfile(USER_ID, new UpdateProfileRequest("  Sol  ", null, null));

    assertThat(response.nickname()).isEqualTo("Sol");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "abcdefghijklmnopqrstu"})
  @DisplayName("닉네임이 비었거나 20자를 넘으면 USER_INVALID_NICKNAME")
  void updateProfile_잘못된닉네임_예외(String nickname) {
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

    assertThatThrownBy(
            () ->
                userService.updateProfile(USER_ID, new UpdateProfileRequest(nickname, null, null)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_INVALID_NICKNAME);
    assertThat(user.getNickname()).isEqualTo("Luna");
  }

  @Test
  @DisplayName("닉네임 20자는 허용한다")
  void updateProfile_닉네임20자_허용() {
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

    userService.updateProfile(
        USER_ID, new UpdateProfileRequest("abcdefghijklmnopqrst", null, null));

    assertThat(user.getNickname()).isEqualTo("abcdefghijklmnopqrst");
  }

  @Test
  @DisplayName("본인이 올린 PROFILE 파일이면 LINKED 처리하고 key 를 저장한다")
  void updateProfile_본인프로필파일_저장() {
    UUID fileId = UUID.randomUUID();
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
    given(fileService.findDetail(fileId))
        .willReturn(Optional.of(file(fileId, USER_ID, FileDomain.PROFILE)));

    UserResponse response =
        userService.updateProfile(USER_ID, new UpdateProfileRequest(null, fileId, null));

    verify(fileService).markLinked(List.of(fileId));
    assertThat(response.profileImageKey()).isEqualTo("profile/2026/11/a.jpg");
  }

  @Test
  @DisplayName("남이 올린 파일이나 PROFILE 이 아닌 파일이면 COMMON_INVALID_REQUEST")
  void updateProfile_남의파일_예외() {
    UUID othersFile = UUID.randomUUID();
    UUID diaryFile = UUID.randomUUID();
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
    given(fileService.findDetail(othersFile))
        .willReturn(Optional.of(file(othersFile, UUID.randomUUID(), FileDomain.PROFILE)));
    given(fileService.findDetail(diaryFile))
        .willReturn(Optional.of(file(diaryFile, USER_ID, FileDomain.DIARY)));

    for (UUID fileId : List.of(othersFile, diaryFile)) {
      assertThatThrownBy(
              () ->
                  userService.updateProfile(USER_ID, new UpdateProfileRequest(null, fileId, null)))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);
    }
    verify(fileService, never()).markLinked(any());
  }

  @Test
  @DisplayName("removeProfileImage=true 면 프로필 이미지 key 를 비운다")
  void updateProfile_이미지제거_null() {
    user.changeProfileImageKey("profile/2026/11/old.jpg");
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

    UserResponse response =
        userService.updateProfile(USER_ID, new UpdateProfileRequest(null, null, true));

    assertThat(response.profileImageKey()).isNull();
  }

  @Test
  @DisplayName("마케팅 수신 값이 바뀌면 marketingUpdatedAt 을 현재 시각으로 갱신한다")
  void updateNotificationSettings_값변경_변경일갱신() {
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

    NotificationSettingsResponse response = userService.updateNotificationSettings(USER_ID, true);

    assertThat(response.marketing()).isTrue();
    assertThat(response.service()).isTrue();
    assertThat(response.marketingUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  @DisplayName("마케팅 수신 값이 그대로면 marketingUpdatedAt 을 바꾸지 않는다")
  void updateNotificationSettings_같은값_변경일유지() {
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

    NotificationSettingsResponse unchanged = userService.updateNotificationSettings(USER_ID, false);
    NotificationSettingsResponse omitted = userService.updateNotificationSettings(USER_ID, null);

    assertThat(unchanged.marketingUpdatedAt()).isNull();
    assertThat(omitted.marketing()).isFalse();
    assertThat(omitted.marketingUpdatedAt()).isNull();
  }

  @Test
  @DisplayName("언어를 바꾸면 locale 에 저장한다")
  void updateLanguage_정상_locale저장() {
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

    userService.updateLanguage(USER_ID, "ko");

    assertThat(user.getLocale()).isEqualTo("ko");
  }

  @Test
  @DisplayName("탈퇴한 회원의 설정 변경은 USER_NOT_FOUND")
  void updateLanguage_탈퇴회원_예외() {
    user.withdraw(NOW);
    given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

    assertThatThrownBy(() -> userService.updateLanguage(USER_ID, "ko"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_NOT_FOUND);
  }

  private static FileDetail file(UUID fileId, UUID uploaderId, FileDomain domain) {
    return new FileDetail(
        fileId, uploaderId, domain, "profile/2026/11/a.jpg", "image/jpeg", FileStatus.PENDING);
  }
}
