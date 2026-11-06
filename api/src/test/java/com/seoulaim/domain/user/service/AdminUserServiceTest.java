package com.seoulaim.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.artist.service.UserBiasService;
import com.seoulaim.domain.diary.dto.UserDiaryStats;
import com.seoulaim.domain.diary.service.DiaryPaymentService;
import com.seoulaim.domain.diary.service.DiaryService;
import com.seoulaim.domain.inquiry.service.RefundRequestQueryService;
import com.seoulaim.domain.user.admin.dto.AdminUserListResponse;
import com.seoulaim.domain.user.dto.UserAccountInfo;
import com.seoulaim.domain.user.entity.UserStatus;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

  @Mock private UserService userService;
  @Mock private UserWithdrawalService userWithdrawalService;
  @Mock private UserBiasService userBiasService;
  @Mock private DiaryService diaryService;
  @Mock private DiaryPaymentService diaryPaymentService;
  @Mock private RefundRequestQueryService refundRequestQueryService;

  private AdminUserService adminUserService;

  @BeforeEach
  void setUp() {
    adminUserService =
        new AdminUserService(
            userService,
            userWithdrawalService,
            userBiasService,
            diaryService,
            diaryPaymentService,
            refundRequestQueryService);
  }

  private static UserAccountInfo account(UUID id, String email, UserStatus status) {
    return new UserAccountInfo(
        id,
        email,
        "Mina",
        null,
        "en",
        status,
        Instant.parse("2026-10-01T00:00:00Z"),
        null,
        null,
        null,
        true,
        null,
        true);
  }

  @Test
  @DisplayName("목록은 이메일을 마스킹하고 다이어리·결제 수와 소문자 status 를 붙인다. status 는 대소문자 무시")
  void getUsers_마스킹과통계() {
    UUID id = UUID.randomUUID();
    given(userService.searchAccounts(UserStatus.ACTIVE, "mina", 0, 50))
        .willReturn(
            new PageImpl<>(
                List.of(account(id, "mina@example.com", UserStatus.ACTIVE)),
                PageRequest.of(0, 50),
                1));
    given(diaryPaymentService.findUserStats(List.of(id)))
        .willReturn(Map.of(id, new UserDiaryStats(2, 1)));

    AdminUserListResponse response = adminUserService.getUsers("mina", "Active", 0, 50);

    assertThat(response.items())
        .containsExactly(
            new AdminUserListResponse.Item(
                id,
                "Mina",
                "mi**@example.com",
                "en",
                Instant.parse("2026-10-01T00:00:00Z"),
                2,
                1,
                "active"));
    assertThat(response.totalElements()).isEqualTo(1);
  }

  @Test
  @DisplayName("없는 status 값과 범위 밖 size 는 400 COMMON_INVALID_REQUEST")
  void getUsers_잘못된파라미터_400() {
    assertThatThrownBy(() -> adminUserService.getUsers(null, "deleted", 0, 50))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);
    assertThatThrownBy(() -> adminUserService.getUsers(null, null, 0, 201))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);
  }

  @Test
  @DisplayName("강퇴 사유는 앞뒤 공백을 지워 넘기고, 비었거나 200자 넘으면 400 이고 탈퇴 처리하지 않는다")
  void withdraw_사유검증() {
    UUID id = UUID.randomUUID();

    adminUserService.withdraw(id, "  환불 악용 반복  ");

    verify(userWithdrawalService).withdrawByAdmin(id, "환불 악용 반복");
    assertThatThrownBy(() -> adminUserService.withdraw(id, " "))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);
    assertThatThrownBy(() -> adminUserService.withdraw(id, "가".repeat(201)))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);
    verify(userWithdrawalService, times(1)).withdrawByAdmin(any(), any());
  }
}
