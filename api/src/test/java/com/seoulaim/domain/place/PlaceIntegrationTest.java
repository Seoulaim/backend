package com.seoulaim.domain.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.seoulaim.domain.artist.entity.Artist;
import com.seoulaim.domain.artist.entity.ArtistGroup;
import com.seoulaim.domain.artist.repository.ArtistGroupRepository;
import com.seoulaim.domain.artist.repository.ArtistRepository;
import com.seoulaim.domain.place.dto.PlaceDetailResponse;
import com.seoulaim.domain.place.entity.ArtistPlace;
import com.seoulaim.domain.place.entity.Place;
import com.seoulaim.domain.place.entity.PlaceCategory;
import com.seoulaim.domain.place.entity.PlaceEvent;
import com.seoulaim.domain.place.entity.PlaceImage;
import com.seoulaim.domain.place.entity.PlaceOpeningHour;
import com.seoulaim.domain.place.entity.Weekday;
import com.seoulaim.domain.place.repository.ArtistPlaceRepository;
import com.seoulaim.domain.place.repository.PlaceEventRepository;
import com.seoulaim.domain.place.repository.PlaceFavoriteRepository;
import com.seoulaim.domain.place.repository.PlaceImageRepository;
import com.seoulaim.domain.place.repository.PlaceOpeningHourRepository;
import com.seoulaim.domain.place.repository.PlaceRepository;
import com.seoulaim.domain.place.service.PlaceFavoriteService;
import com.seoulaim.domain.place.service.PlaceService;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.repository.UserRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
class PlaceIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  private static final GeometryFactory GEOMETRY = new GeometryFactory(new PrecisionModel(), 4326);
  // KST 2026-10-29 (목) 12:00
  private static final Instant NOON_KST = Instant.parse("2026-10-29T03:00:00Z");

  @MockitoBean private Clock clock;

  @Autowired private PlaceService placeService;
  @Autowired private PlaceFavoriteService placeFavoriteService;
  @Autowired private PlaceRepository placeRepository;
  @Autowired private PlaceImageRepository placeImageRepository;
  @Autowired private PlaceEventRepository placeEventRepository;
  @Autowired private PlaceOpeningHourRepository placeOpeningHourRepository;
  @Autowired private ArtistPlaceRepository artistPlaceRepository;
  @Autowired private PlaceFavoriteRepository placeFavoriteRepository;
  @Autowired private ArtistRepository artistRepository;
  @Autowired private ArtistGroupRepository artistGroupRepository;
  @Autowired private UserRepository userRepository;

  private UUID userId;
  private Place cafe;

  @BeforeEach
  void setUp() {
    placeFavoriteRepository.deleteAllInBatch();
    artistPlaceRepository.deleteAllInBatch();
    placeEventRepository.deleteAllInBatch();
    placeOpeningHourRepository.deleteAllInBatch();
    placeImageRepository.deleteAllInBatch();
    placeRepository.deleteAllInBatch();
    artistRepository.deleteAllInBatch();
    artistGroupRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();

    given(clock.instant()).willReturn(NOON_KST);

    userId = userRepository.save(User.createWithEmail("luna@example.com", "hash", "Luna")).getId();
    cafe =
        placeRepository.save(
            Place.create(
                "어니언 성수",
                PlaceCategory.FANDOM_PLACE,
                "서울 성동구 아차산로9길 8",
                GEOMETRY.createPoint(new Coordinate(127.0581, 37.5447)),
                "성동구",
                null,
                null));
    saveDailyHours(cafe, LocalTime.of(10, 0), LocalTime.of(22, 0), null, null);
    placeImageRepository.save(PlaceImage.create(cafe, "place/b.jpg", 2));
    placeImageRepository.save(PlaceImage.create(cafe, "place/a.jpg", 1));

    ArtistGroup ive =
        artistGroupRepository.save(ArtistGroup.create("IVE", "아이브", null, null, null, null, 1));
    Artist liz = artistRepository.save(Artist.create(ive, "LIZ", "리즈", null, null, null, null, 1));
    artistPlaceRepository.save(ArtistPlace.ofArtist(cafe, liz, "멤버 연결"));
  }

  @Test
  @DisplayName("장소 상세는 위치·이미지 순서·아티스트 연결·영업 여부를 함께 반환한다")
  void getPlaceDetail_정상조회() {
    PlaceDetailResponse result = placeService.getPlaceDetail(userId, cafe.getId());

    assertThat(result.location().latitude()).isEqualTo(37.5447);
    assertThat(result.location().longitude()).isEqualTo(127.0581);
    assertThat(result.imageKeys()).containsExactly("place/a.jpg", "place/b.jpg");
    assertThat(result.artists()).hasSize(1);
    assertThat(result.artists().getFirst().artistNameEn()).isEqualTo("LIZ");
    assertThat(result.artists().getFirst().artistGroupNameEn()).isEqualTo("IVE");
    assertThat(result.opensAt()).isEqualTo(LocalTime.of(10, 0));
    assertThat(result.closesAt()).isEqualTo(LocalTime.of(22, 0));
    assertThat(result.isOpenNow()).isTrue();
    assertThat(result.openingHours()).hasSize(7);
    assertThat(result.openingHours().getFirst().dayOfWeek()).isEqualTo(Weekday.MON);
    assertThat(result.ongoingEvent()).isNull();
    assertThat(result.isFavorite()).isFalse();
  }

  @Test
  @DisplayName("진행 중 이벤트에 운영시간이 있으면 이벤트 시간 기준으로 영업 여부를 계산한다")
  void getPlaceDetail_진행중이벤트_이벤트시간우선() {
    placeEventRepository.save(
        PlaceEvent.create(
            cafe,
            "생일 카페",
            LocalDate.of(2026, 10, 25),
            LocalDate.of(2026, 10, 31),
            LocalTime.of(14, 0),
            LocalTime.of(20, 0)));

    PlaceDetailResponse result = placeService.getPlaceDetail(userId, cafe.getId());

    assertThat(result.ongoingEvent().title()).isEqualTo("생일 카페");
    assertThat(result.opensAt()).isEqualTo(LocalTime.of(14, 0));
    assertThat(result.isOpenNow()).isFalse();
  }

  @Test
  @DisplayName("오늘이 휴무 요일이면 오늘 영업시간은 null 이고 영업 중이 아니다")
  void getPlaceDetail_휴무요일() {
    placeOpeningHourRepository.deleteAllInBatch();
    for (Weekday day : Weekday.values()) {
      placeOpeningHourRepository.save(
          day == Weekday.THU
              ? PlaceOpeningHour.closedOn(cafe, day)
              : PlaceOpeningHour.open(
                  cafe, day, LocalTime.of(10, 0), LocalTime.of(22, 0), null, null));
    }

    PlaceDetailResponse result = placeService.getPlaceDetail(userId, cafe.getId());

    assertThat(result.opensAt()).isNull();
    assertThat(result.closesAt()).isNull();
    assertThat(result.isOpenNow()).isFalse();
  }

  @Test
  @DisplayName("브레이크 시간에는 영업 중이 아니다")
  void getPlaceDetail_브레이크() {
    placeOpeningHourRepository.deleteAllInBatch();
    saveDailyHours(
        cafe, LocalTime.of(10, 0), LocalTime.of(22, 0), LocalTime.of(11, 30), LocalTime.of(12, 30));

    PlaceDetailResponse result = placeService.getPlaceDetail(userId, cafe.getId());

    assertThat(result.isOpenNow()).isFalse();
    assertThat(result.openingHours().getFirst().breakStartsAt()).isEqualTo(LocalTime.of(11, 30));
  }

  @Test
  @DisplayName("영업시간이 없는 장소는 언제든 영업 중이고 openingHours 는 null 이다")
  void getPlaceDetail_영업시간없음() {
    placeOpeningHourRepository.deleteAllInBatch();

    PlaceDetailResponse result = placeService.getPlaceDetail(userId, cafe.getId());

    assertThat(result.opensAt()).isNull();
    assertThat(result.isOpenNow()).isTrue();
    assertThat(result.openingHours()).isNull();
  }

  @Test
  @DisplayName("숨김 장소는 상세 조회·즐겨찾기 시 PLACE_NOT_FOUND 예외가 발생한다")
  void getPlaceDetail_숨김장소_예외() {
    cafe.changeVisibility(false);
    placeRepository.save(cafe);

    assertThatThrownBy(() -> placeService.getPlaceDetail(userId, cafe.getId()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PLACE_NOT_FOUND);
    assertThatThrownBy(() -> placeFavoriteService.addFavorite(userId, cafe.getId()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PLACE_NOT_FOUND);
  }

  @Test
  @DisplayName("존재하지 않는 장소면 PLACE_NOT_FOUND 예외가 발생한다")
  void getPlaceDetail_없는장소_예외() {
    assertThatThrownBy(() -> placeService.getPlaceDetail(userId, UUID.randomUUID()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PLACE_NOT_FOUND);
  }

  @Test
  @DisplayName("즐겨찾기를 두 번 등록해도 한 건만 저장되고, 두 번 해제해도 예외가 없다")
  void favorite_반복호출_결과동일() {
    placeFavoriteService.addFavorite(userId, cafe.getId());
    placeFavoriteService.addFavorite(userId, cafe.getId());

    assertThat(placeFavoriteRepository.count()).isEqualTo(1);
    assertThat(placeService.getPlaceDetail(userId, cafe.getId()).isFavorite()).isTrue();

    placeFavoriteService.removeFavorite(userId, cafe.getId());
    placeFavoriteService.removeFavorite(userId, cafe.getId());

    assertThat(placeFavoriteRepository.count()).isZero();
  }

  @Test
  @DisplayName("존재하지 않는 장소를 즐겨찾기하면 PLACE_NOT_FOUND 예외가 발생한다")
  void addFavorite_없는장소_예외() {
    assertThatThrownBy(() -> placeFavoriteService.addFavorite(userId, UUID.randomUUID()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PLACE_NOT_FOUND);
  }

  private void saveDailyHours(
      Place place,
      LocalTime opensAt,
      LocalTime closesAt,
      LocalTime breakStartsAt,
      LocalTime breakEndsAt) {
    for (Weekday day : Weekday.values()) {
      placeOpeningHourRepository.save(
          PlaceOpeningHour.open(place, day, opensAt, closesAt, breakStartsAt, breakEndsAt));
    }
  }
}
