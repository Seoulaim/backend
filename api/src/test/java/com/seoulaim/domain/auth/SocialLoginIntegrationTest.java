package com.seoulaim.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.seoulaim.domain.auth.dto.SocialLoginResponse;
import com.seoulaim.domain.auth.dto.TokenResponse;
import com.seoulaim.domain.auth.service.GoogleIdTokenVerifier;
import com.seoulaim.domain.auth.service.SocialAuthService;
import com.seoulaim.domain.auth.service.SocialProfile;
import com.seoulaim.domain.user.entity.SocialProvider;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.repository.UserRepository;
import com.seoulaim.domain.user.repository.UserSocialAccountRepository;
import com.seoulaim.domain.user.service.UserService;
import com.seoulaim.domain.user.service.UserWithdrawalService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** 구글 로그인·가입·자동 연결·탈퇴 후 재가입을 실제 DB·Redis 로 검증. 구글 토큰 검증만 mock */
@Tag("integration")
@SpringBootTest
@Testcontainers
class SocialLoginIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @Container
  @ServiceConnection(name = "redis")
  static GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  private static final String ID_TOKEN = "google-id-token";
  private static final String SUB = "google-sub-1";

  @MockitoBean private GoogleIdTokenVerifier googleIdTokenVerifier;

  @Autowired private SocialAuthService socialAuthService;
  @Autowired private UserService userService;
  @Autowired private UserWithdrawalService userWithdrawalService;
  @Autowired private UserRepository userRepository;
  @Autowired private UserSocialAccountRepository userSocialAccountRepository;

  @BeforeEach
  void setUp() {
    userSocialAccountRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();
  }

  @Test
  @DisplayName("처음 로그인하면 signupRequired 와 이메일·추천 닉네임을 주고, 가입 후에는 바로 로그인된다")
  void 신규가입_후_로그인() {
    givenProfile("luna@gmail.com", "Luna Kim");

    SocialLoginResponse first = socialAuthService.loginWithGoogle(ID_TOKEN);
    assertThat(first.signupRequired()).isTrue();
    assertThat(first.tokens()).isNull();
    assertThat(first.email()).isEqualTo("luna@gmail.com");
    assertThat(first.suggestedNickname()).isEqualTo("Luna Kim");

    TokenResponse signedUp = socialAuthService.signupWithGoogle(ID_TOKEN, " Luna ");
    assertThat(signedUp.accessToken()).isNotBlank();
    User user = userRepository.findAll().getFirst();
    assertThat(user.getNickname()).isEqualTo("Luna");
    assertThat(user.getEmail()).isEqualTo("luna@gmail.com");
    assertThat(user.getPasswordHash()).isNull();

    SocialLoginResponse second = socialAuthService.loginWithGoogle(ID_TOKEN);
    assertThat(second.signupRequired()).isFalse();
    assertThat(second.tokens().accessToken()).isNotBlank();
  }

  @Test
  @DisplayName("구글이 인증한 이메일이 기존 이메일 회원과 같으면 자동 연결 후 로그인된다")
  void 기존이메일회원_자동연결() {
    User existing = userService.createUser("luna@gmail.com", "hash", "Luna");
    givenProfile("Luna@Gmail.com", "Luna Kim");

    SocialLoginResponse result = socialAuthService.loginWithGoogle(ID_TOKEN);

    assertThat(result.signupRequired()).isFalse();
    assertThat(userRepository.count()).isEqualTo(1);
    assertThat(
            userSocialAccountRepository
                .findWithUser(SocialProvider.GOOGLE, SUB)
                .orElseThrow()
                .getUser()
                .getId())
        .isEqualTo(existing.getId());
  }

  @Test
  @DisplayName("인증되지 않은 이메일이면 기존 회원과 연결하지 않고 이메일 없이 가입한다")
  void 미인증이메일_연결안함() {
    userService.createUser("luna@gmail.com", "hash", "Luna");
    givenProfile(null, null);

    SocialLoginResponse result = socialAuthService.loginWithGoogle(ID_TOKEN);
    assertThat(result.signupRequired()).isTrue();
    assertThat(result.suggestedNickname()).isEmpty();

    socialAuthService.signupWithGoogle(ID_TOKEN, "Luna2");
    assertThat(userRepository.count()).isEqualTo(2);
  }

  @Test
  @DisplayName("가입 API 를 두 번 보내도 회원은 하나만 생긴다")
  void 가입_중복요청() {
    givenProfile("luna@gmail.com", "Luna");

    socialAuthService.signupWithGoogle(ID_TOKEN, "Luna");
    socialAuthService.signupWithGoogle(ID_TOKEN, "Luna");

    assertThat(userRepository.count()).isEqualTo(1);
    assertThat(userSocialAccountRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("탈퇴하면 소셜 연결이 지워져 같은 구글 계정으로 새로 가입해야 한다")
  void 탈퇴후_재가입() {
    givenProfile("luna@gmail.com", "Luna");
    socialAuthService.signupWithGoogle(ID_TOKEN, "Luna");
    User user = userRepository.findAll().getFirst();

    userWithdrawalService.withdraw(user.getId());

    assertThat(userSocialAccountRepository.count()).isZero();
    assertThat(socialAuthService.loginWithGoogle(ID_TOKEN).signupRequired()).isTrue();
  }

  @Test
  @DisplayName("구글 토큰 검증에 실패하면 AUTH_INVALID_SOCIAL_TOKEN")
  void 토큰검증실패() {
    given(googleIdTokenVerifier.verify("bad"))
        .willThrow(new BusinessException(ErrorCode.AUTH_INVALID_SOCIAL_TOKEN));

    assertThatThrownBy(() -> socialAuthService.loginWithGoogle("bad"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_INVALID_SOCIAL_TOKEN);
  }

  private void givenProfile(String verifiedEmail, String name) {
    given(googleIdTokenVerifier.verify(ID_TOKEN))
        .willReturn(new SocialProfile(SocialProvider.GOOGLE, SUB, verifiedEmail, name));
  }
}
