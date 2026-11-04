package com.seoulaim.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.seoulaim.domain.artist.entity.Artist;
import com.seoulaim.domain.artist.entity.ArtistGroup;
import com.seoulaim.domain.artist.entity.UserBias;
import com.seoulaim.domain.artist.repository.ArtistGroupRepository;
import com.seoulaim.domain.artist.repository.ArtistRepository;
import com.seoulaim.domain.artist.repository.UserBiasRepository;
import com.seoulaim.domain.auth.dto.TokenResponse;
import com.seoulaim.domain.auth.service.AuthService;
import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryPayment;
import com.seoulaim.domain.diary.repository.DiaryPaymentRepository;
import com.seoulaim.domain.diary.repository.DiaryRepository;
import com.seoulaim.domain.place.entity.Place;
import com.seoulaim.domain.place.entity.PlaceCategory;
import com.seoulaim.domain.place.repository.PlaceFavoriteRepository;
import com.seoulaim.domain.place.repository.PlaceRepository;
import com.seoulaim.domain.place.service.PlaceFavoriteService;
import com.seoulaim.domain.plan.entity.PlanDay;
import com.seoulaim.domain.plan.entity.TravelPlan;
import com.seoulaim.domain.plan.repository.PlanDayRepository;
import com.seoulaim.domain.plan.repository.TravelPlanRepository;
import com.seoulaim.domain.survey.entity.SurveyResponse;
import com.seoulaim.domain.survey.repository.SurveyResponseRepository;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.entity.UserStatus;
import com.seoulaim.domain.user.repository.UserRepository;
import com.seoulaim.domain.user.service.UserService;
import com.seoulaim.domain.user.service.UserWithdrawalService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** 탈퇴 → 로그인·토큰 재발급 실패, 도메인별 데이터 정리(결제 기록은 유지)를 실제 DB·Redis 로 검증한다. */
@Tag("integration")
@SpringBootTest
@Testcontainers
class UserWithdrawalIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @Container
  @ServiceConnection(name = "redis")
  static GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  private static final String EMAIL = "luna@example.com";
  private static final String PASSWORD = "seoulaim1234";
  private static final GeometryFactory GEOMETRY = new GeometryFactory(new PrecisionModel(), 4326);

  @Autowired private UserWithdrawalService userWithdrawalService;
  @Autowired private UserService userService;
  @Autowired private AuthService authService;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private UserRepository userRepository;
  @Autowired private ArtistGroupRepository artistGroupRepository;
  @Autowired private ArtistRepository artistRepository;
  @Autowired private UserBiasRepository userBiasRepository;
  @Autowired private TravelPlanRepository travelPlanRepository;
  @Autowired private PlanDayRepository planDayRepository;
  @Autowired private DiaryRepository diaryRepository;
  @Autowired private DiaryPaymentRepository diaryPaymentRepository;
  @Autowired private PlaceRepository placeRepository;
  @Autowired private PlaceFavoriteRepository placeFavoriteRepository;
  @Autowired private PlaceFavoriteService placeFavoriteService;
  @Autowired private SurveyResponseRepository surveyResponseRepository;

  @Test
  @DisplayName(
      "탈퇴하면 로그인·토큰 재발급이 실패하고 bias·즐겨찾기·일정은 삭제, 다이어리는 soft delete, " + "결제는 남고 설문 응답은 이메일만 익명화된다")
  void withdraw_탈퇴후_로그인실패_데이터정리() {
    User user = userService.createUser(EMAIL, passwordEncoder.encode(PASSWORD), "Luna");
    UUID userId = user.getId();
    TokenResponse tokens = authService.login(EMAIL, PASSWORD);

    ArtistGroup group =
        artistGroupRepository.save(ArtistGroup.create("ATEEZ", "에이티즈", null, null, null, null, 1));
    Artist san = artistRepository.save(Artist.create(group, "SAN", "산", null, null, null, null, 1));
    userBiasRepository.save(UserBias.create(user, san));
    LocalDate start = LocalDate.of(2026, 12, 1);
    TravelPlan plan = travelPlanRepository.save(TravelPlan.create(user, start, start.plusDays(1)));
    planDayRepository.save(PlanDay.create(plan, 1, start));
    Diary diary = diaryRepository.save(Diary.create(userId, false));
    DiaryPayment payment =
        DiaryPayment.createPending(
            diary.getId(), userId, "DIA-20261104-ABCDEFGHIJ", 3000, Instant.now());
    payment.markPaid("toss-key", Instant.now());
    diaryPaymentRepository.save(payment);
    Place cafe =
        placeRepository.save(
            Place.create(
                "어니언 성수",
                PlaceCategory.FANDOM_PLACE,
                "서울 성동구 아차산로9길 8",
                GEOMETRY.createPoint(new Coordinate(127.0581, 37.5447)),
                "성동구",
                null,
                null));
    placeFavoriteService.addFavorite(userId, cafe.getId());
    // 다른 회원의 즐겨찾기는 남아야 한다
    UUID otherId = userService.createUser("sol@example.com", "hash", "Sol").getId();
    placeFavoriteService.addFavorite(otherId, cafe.getId());
    Map<String, Object> answers = Map.of("q1", 5);
    SurveyResponse surveyResponse =
        surveyResponseRepository.save(
            SurveyResponse.create(userId, EMAIL, Map.of("dayCount", 2), answers, Instant.now()));

    userWithdrawalService.withdraw(userId);

    // 기존 로그인 실패(없는 이메일·비밀번호 불일치)와 같은 응답
    assertThatThrownBy(() -> authService.login(EMAIL, PASSWORD))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
    assertThatThrownBy(() -> authService.refresh(tokens.refreshToken()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_INVALID_TOKEN);

    User withdrawn = userRepository.findById(userId).orElseThrow();
    assertThat(withdrawn.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
    assertThat(withdrawn.getEmail()).isNull();
    assertThat(userBiasRepository.findAllWithArtistByUserId(userId)).isEmpty();
    assertThat(travelPlanRepository.existsByUserId(userId)).isFalse();
    assertThat(diaryRepository.findById(diary.getId()).orElseThrow().isDeleted()).isTrue();
    assertThat(diaryPaymentRepository.findById(payment.getId()).orElseThrow().isPaid()).isTrue();
    assertThat(placeFavoriteRepository.existsByUserIdAndPlaceId(userId, cafe.getId())).isFalse();
    assertThat(placeFavoriteRepository.existsByUserIdAndPlaceId(otherId, cafe.getId())).isTrue();
    SurveyResponse anonymized =
        surveyResponseRepository.findById(surveyResponse.getId()).orElseThrow();
    assertThat(anonymized.getEmail()).isEqualTo(SurveyResponse.WITHDRAWN_EMAIL);
    assertThat(anonymized.getUserId()).isEqualTo(userId);
    assertThat(anonymized.getAnswers()).isEqualTo(answers);

    // 다시 호출해도 오류 없음 (멱등), 같은 이메일로 재가입 가능
    userWithdrawalService.withdraw(userId);
    User rejoined = userService.createUser(EMAIL, passwordEncoder.encode(PASSWORD), "Luna2");
    assertThat(rejoined.getId()).isNotEqualTo(userId);
  }
}
