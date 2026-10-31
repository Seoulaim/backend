package com.seoulaim.domain.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.seoulaim.domain.artist.entity.Artist;
import com.seoulaim.domain.artist.entity.ArtistGroup;
import com.seoulaim.domain.artist.repository.ArtistGroupRepository;
import com.seoulaim.domain.artist.repository.ArtistRepository;
import com.seoulaim.domain.place.entity.ArtistPlace;
import com.seoulaim.domain.place.entity.Place;
import com.seoulaim.domain.place.entity.PlaceCategory;
import com.seoulaim.domain.place.entity.PlaceEvent;
import com.seoulaim.domain.place.repository.ArtistPlaceRepository;
import com.seoulaim.domain.place.repository.PlaceEventRepository;
import com.seoulaim.domain.place.repository.PlaceRepository;
import com.seoulaim.domain.plan.dto.CreatePlanRequest;
import com.seoulaim.domain.plan.dto.PlanDetailResponse;
import com.seoulaim.domain.plan.dto.PlanDetailResponse.PlanDayResponse;
import com.seoulaim.domain.plan.dto.PlanDetailResponse.PlanItemResponse;
import com.seoulaim.domain.plan.dto.PlanSummaryResponse;
import com.seoulaim.domain.plan.dto.RecommendDatesRequest;
import com.seoulaim.domain.plan.repository.PlanDayRepository;
import com.seoulaim.domain.plan.repository.PlanItemRepository;
import com.seoulaim.domain.plan.repository.TravelPlanRepository;
import com.seoulaim.domain.plan.service.PlanService;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.repository.UserRepository;
import com.seoulaim.global.common.CursorPageResponse;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@SpringBootTest
@Testcontainers
class PlanIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  private static final GeometryFactory GEOMETRY = new GeometryFactory(new PrecisionModel(), 4326);
  // KST 2026-10-29 12:00
  private static final Instant NOW = Instant.parse("2026-10-29T03:00:00Z");

  @MockitoBean private Clock clock;

  @Autowired private PlanService planService;
  @Autowired private PlanItemRepository planItemRepository;
  @Autowired private PlanDayRepository planDayRepository;
  @Autowired private TravelPlanRepository travelPlanRepository;
  @Autowired private ArtistPlaceRepository artistPlaceRepository;
  @Autowired private PlaceEventRepository placeEventRepository;
  @Autowired private PlaceRepository placeRepository;
  @Autowired private ArtistRepository artistRepository;
  @Autowired private ArtistGroupRepository artistGroupRepository;
  @Autowired private UserRepository userRepository;

  private UUID userId;
  private UUID sanId;

  @BeforeEach
  void setUp() {
    planItemRepository.deleteAllInBatch();
    planDayRepository.deleteAllInBatch();
    travelPlanRepository.deleteAllInBatch();
    artistPlaceRepository.deleteAllInBatch();
    placeEventRepository.deleteAllInBatch();
    placeRepository.deleteAllInBatch();
    artistRepository.deleteAllInBatch();
    artistGroupRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();

    given(clock.instant()).willReturn(NOW);

    userId = userRepository.save(User.createWithEmail("luna@example.com", "hash", "Luna")).getId();
    ArtistGroup ateez =
        artistGroupRepository.save(ArtistGroup.create("ATEEZ", "에이티즈", null, null, null, null, 1));
    Artist san = artistRepository.save(Artist.create(ateez, "SAN", "산", null, null, null, null, 1));
    sanId = san.getId();

    Place cafe =
        placeRepository.save(
            Place.create(
                "어니언 성수",
                PlaceCategory.VISIT_SPOT,
                "서울 성동구 아차산로9길 8",
                GEOMETRY.createPoint(new Coordinate(127.0581, 37.5447)),
                "성동구",
                LocalTime.of(10, 0),
                LocalTime.of(22, 0),
                null,
                null));
    Place birthdayCafe =
        placeRepository.save(
            Place.create(
                "테스트 생일카페",
                PlaceCategory.BIRTHDAY_CAFE,
                "서울 마포구 테스트로 1",
                GEOMETRY.createPoint(new Coordinate(126.9236, 37.5563)),
                "마포구",
                LocalTime.of(10, 0),
                LocalTime.of(21, 0),
                null,
                null));
    artistPlaceRepository.save(ArtistPlace.ofGroup(cafe, ateez, "그룹 연결"));
    artistPlaceRepository.save(ArtistPlace.ofArtist(birthdayCafe, san, "멤버 연결"));
    placeEventRepository.save(
        PlaceEvent.create(
            birthdayCafe,
            "생일 이벤트",
            LocalDate.of(2026, 10, 30),
            LocalDate.of(2026, 11, 1),
            LocalTime.of(11, 0),
            LocalTime.of(20, 0)));
  }

  @Test
  @DisplayName("일정을 생성하면 점수 순으로 첫 장소를 정하고, 여는 시각까지 기다린 도착 시각을 저장한다")
  void createPlan_정상_순서와도착시각() {
    PlanDetailResponse result =
        planService.createPlan(
            userId,
            new CreatePlanRequest(
                LocalDate.of(2026, 10, 30),
                LocalDate.of(2026, 11, 3),
                List.of(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 10, 31)),
                List.of(sanId),
                null));

    assertThat(result.title()).isEqualTo("Luna's Travel");
    assertThat(result.days())
        .extracting(PlanDayResponse::visitDate)
        .containsExactly(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 2));

    List<PlanItemResponse> firstDay = result.days().getFirst().items();
    assertThat(firstDay)
        .extracting(item -> item.place().name())
        .containsExactly("테스트 생일카페", "어니언 성수");
    assertThat(firstDay.getFirst().arrivalTime()).isEqualTo(LocalTime.of(11, 0));
    // 이미 배정한 장소 제외 + 생일카페 이벤트 종료 → 둘째 날은 빈 목록
    assertThat(result.days().get(1).items()).isEmpty();
  }

  @Test
  @DisplayName("이벤트가 없는 날에는 생일카페를 후보에서 제외한다")
  void createPlan_이벤트없는날_생일카페제외() {
    PlanDetailResponse result =
        planService.createPlan(
            userId,
            new CreatePlanRequest(
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 3),
                List.of(LocalDate.of(2026, 11, 2)),
                List.of(sanId),
                null));

    List<PlanItemResponse> items = result.days().getFirst().items();
    assertThat(items).extracting(item -> item.place().name()).containsExactly("어니언 성수");
    assertThat(items.getFirst().arrivalTime()).isEqualTo(LocalTime.of(10, 0));
  }

  @Test
  @DisplayName("시작일이 오늘(KST) 이전이면 PLAN_INVALID_PERIOD 예외가 발생한다")
  void createPlan_과거날짜_예외() {
    assertThatThrownBy(
            () ->
                planService.createPlan(
                    userId,
                    new CreatePlanRequest(
                        LocalDate.of(2026, 10, 28),
                        LocalDate.of(2026, 10, 30),
                        List.of(LocalDate.of(2026, 10, 30)),
                        List.of(sanId),
                        null)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PLAN_INVALID_PERIOD);
  }

  @Test
  @DisplayName("방문일이 여행 기간 밖이면 PLAN_VISIT_DATE_OUT_OF_PERIOD 예외가 발생한다")
  void createPlan_기간밖방문일_예외() {
    assertThatThrownBy(
            () ->
                planService.createPlan(
                    userId,
                    new CreatePlanRequest(
                        LocalDate.of(2026, 10, 30),
                        LocalDate.of(2026, 11, 3),
                        List.of(LocalDate.of(2026, 11, 5)),
                        List.of(sanId),
                        null)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PLAN_VISIT_DATE_OUT_OF_PERIOD);
  }

  @Test
  @DisplayName("다른 사용자의 일정을 조회하면 없는 일정과 같은 PLAN_NOT_FOUND 예외가 발생한다")
  void getPlanDetail_다른사용자_예외() {
    UUID planId = createSamplePlan();
    UUID otherUserId =
        userRepository.save(User.createWithEmail("other@example.com", "hash", "Other")).getId();

    assertThatThrownBy(() -> planService.getPlanDetail(otherUserId, planId))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PLAN_NOT_FOUND);
  }

  @Test
  @DisplayName("일정을 삭제하면 하위 날짜·장소까지 삭제되고 다시 조회할 수 없다")
  void deletePlan_하위데이터삭제() {
    UUID planId = createSamplePlan();

    planService.deletePlan(userId, planId);

    assertThat(planItemRepository.count()).isZero();
    assertThat(planDayRepository.count()).isZero();
    assertThatThrownBy(() -> planService.getPlanDetail(userId, planId))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PLAN_NOT_FOUND);
  }

  @Test
  @DisplayName("일정 목록은 최신 생성순이고 nextCursor 로 다음 페이지를 이어서 조회한다")
  void findPlans_최신순_커서페이징() {
    UUID older = createSamplePlan();
    UUID newer = createSamplePlan();

    CursorPageResponse<PlanSummaryResponse> first = planService.findPlans(userId, null, 1);
    CursorPageResponse<PlanSummaryResponse> second =
        planService.findPlans(userId, first.nextCursor(), 1);

    assertThat(first.items()).extracting(PlanSummaryResponse::id).containsExactly(newer);
    assertThat(first.items().getFirst().dayCount()).isEqualTo(1);
    assertThat(first.nextCursor()).isNotNull();
    assertThat(second.items()).extracting(PlanSummaryResponse::id).containsExactly(older);
    assertThat(second.nextCursor()).isNull();
  }

  @Test
  @DisplayName("날짜 추천은 bias 관련 이벤트가 열리는 날을 우선하고, 동점이면 빠른 날을 고른다")
  void recommendDates_이벤트많은날우선() {
    List<LocalDate> dates =
        planService
            .recommendDates(
                userId,
                new RecommendDatesRequest(
                    LocalDate.of(2026, 10, 29), LocalDate.of(2026, 11, 3), 2, List.of(sanId)))
            .dates();

    assertThat(dates).containsExactly(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 10, 31));
  }

  // 11/02 하루, 어니언 1곳짜리 일정
  private UUID createSamplePlan() {
    return planService
        .createPlan(
            userId,
            new CreatePlanRequest(
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 3),
                List.of(LocalDate.of(2026, 11, 2)),
                List.of(sanId),
                null))
        .id();
  }
}
