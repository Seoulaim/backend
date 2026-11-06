package com.seoulaim.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.seoulaim.domain.auth.dto.TokenResponse;
import com.seoulaim.domain.auth.service.AuthService;
import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryDownload;
import com.seoulaim.domain.diary.entity.DiaryDownloadFormat;
import com.seoulaim.domain.diary.entity.DiaryPayment;
import com.seoulaim.domain.diary.repository.DiaryDownloadRepository;
import com.seoulaim.domain.diary.repository.DiaryPaymentRepository;
import com.seoulaim.domain.diary.repository.DiaryRepository;
import com.seoulaim.domain.user.admin.dto.AdminUserDetailResponse;
import com.seoulaim.domain.user.admin.dto.AdminUserListResponse;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.entity.UserStatus;
import com.seoulaim.domain.user.entity.WithdrawnBy;
import com.seoulaim.domain.user.repository.UserRepository;
import com.seoulaim.domain.user.service.AdminUserService;
import com.seoulaim.domain.user.service.UserService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** 어드민 회원 목록 검색·상세·강퇴(→ 로그인·토큰 재발급 실패)를 실제 DB·Redis 로 검증한다. */
@Tag("integration")
@SpringBootTest
@Testcontainers
class AdminUserIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @Container
  @ServiceConnection(name = "redis")
  static GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  private static final String PASSWORD = "seoulaim1234";

  @Autowired private AdminUserService adminUserService;
  @Autowired private UserService userService;
  @Autowired private AuthService authService;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private UserRepository userRepository;
  @Autowired private DiaryRepository diaryRepository;
  @Autowired private DiaryPaymentRepository diaryPaymentRepository;
  @Autowired private DiaryDownloadRepository diaryDownloadRepository;

  @Test
  @DisplayName("강퇴하면 리프레시·로그인이 실패하고 사유·주체(ADMIN)가 남으며, 다시 강퇴하면 409")
  void withdraw_강퇴후_리프레시실패() {
    String email = "rin_" + UUID.randomUUID() + "@example.com";
    User user = userService.createUser(email, passwordEncoder.encode(PASSWORD), "Rin");
    TokenResponse tokens = authService.login(email, PASSWORD);
    Diary diary = diaryRepository.save(Diary.create(user.getId(), true));

    adminUserService.withdraw(user.getId(), "환불 악용 반복");

    assertThatThrownBy(() -> authService.refresh(tokens.refreshToken()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_INVALID_TOKEN);
    assertThatThrownBy(() -> authService.login(email, PASSWORD))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
    User withdrawn = userRepository.findById(user.getId()).orElseThrow();
    assertThat(withdrawn.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
    assertThat(withdrawn.getWithdrawnBy()).isEqualTo(WithdrawnBy.ADMIN);
    assertThat(withdrawn.getWithdrawReason()).isEqualTo("환불 악용 반복");
    assertThat(diaryRepository.findById(diary.getId()).orElseThrow().isDeleted()).isTrue();

    AdminUserDetailResponse detail = adminUserService.getUser(user.getId());
    assertThat(detail.status()).isEqualTo("withdrawn");
    assertThat(detail.withdrawnBy()).isEqualTo("admin");
    assertThat(detail.email()).isNull();
    assertThatThrownBy(() -> adminUserService.withdraw(user.getId(), "again"))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_ALREADY_WITHDRAWN);
    assertThatThrownBy(() -> adminUserService.withdraw(UUID.randomUUID(), "x"))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_NOT_FOUND);
  }

  @Test
  @DisplayName("목록은 마스킹 전 이메일·닉네임으로 검색하고 status 로 거르며, 상세는 다이어리·결제·다운로드 수를 준다")
  void getUsers_검색과상세() {
    String tag = UUID.randomUUID().toString().substring(0, 8);
    User sora = userService.createUser("sora." + tag + "@example.com", "hash", "Sora" + tag);
    User yuki = userService.createUser("yuki." + tag + "@example.com", "hash", "Yuki" + tag);
    adminUserService.withdraw(yuki.getId(), "본인 요청");

    Diary diary = Diary.create(sora.getId(), false);
    diary.markPaid();
    diary.finalizeWith(List.of(), Map.of(), 3, Instant.parse("2026-11-05T00:00:00Z"));
    diaryRepository.save(diary);
    DiaryPayment payment =
        DiaryPayment.createPending(
            diary.getId(),
            sora.getId(),
            "DIA-20261105-" + tag.toUpperCase() + "AB",
            3000,
            Instant.now());
    payment.markPaid("pk", Instant.parse("2026-11-04T00:00:00Z"));
    diaryPaymentRepository.save(payment);
    diaryDownloadRepository.save(
        DiaryDownload.create(diary.getId(), sora.getId(), DiaryDownloadFormat.PDF, 4));

    AdminUserListResponse byEmail = adminUserService.getUsers("SORA." + tag, null, 0, 50);
    assertThat(byEmail.items()).hasSize(1);
    AdminUserListResponse.Item item = byEmail.items().getFirst();
    assertThat(item.id()).isEqualTo(sora.getId());
    assertThat(item.emailMasked()).startsWith("so").endsWith("@example.com").contains("**");
    assertThat(item.diaryCount()).isEqualTo(1);
    assertThat(item.paidPaymentCount()).isEqualTo(1);
    // 탈퇴 회원은 이메일이 지워져 닉네임으로만 찾는다
    assertThat(adminUserService.getUsers("yuki." + tag, null, 0, 50).items()).isEmpty();
    assertThat(adminUserService.getUsers(tag, "withdrawn", 0, 50).items())
        .extracting(AdminUserListResponse.Item::id)
        .containsExactly(yuki.getId());
    assertThat(adminUserService.getUsers(tag, "active", 0, 50).items())
        .extracting(AdminUserListResponse.Item::id)
        .containsExactly(sora.getId());

    AdminUserDetailResponse detail = adminUserService.getUser(sora.getId());
    assertThat(detail.email()).isEqualTo("sora." + tag + "@example.com");
    assertThat(detail.diaries()).hasSize(1);
    assertThat(detail.diaries().getFirst().downloadCount()).isEqualTo(1);
    assertThat(detail.diaries().getFirst().finalized()).isTrue();
    assertThat(detail.diaries().getFirst().saveCount()).isEqualTo(1);
    assertThat(detail.payments())
        .extracting(AdminUserDetailResponse.Payment::status)
        .containsExactly("paid");
    assertThat(detail.biases()).isEmpty();
    assertThat(detail.stats()).isEqualTo(new AdminUserDetailResponse.Stats(0, 0, 0));
  }
}
