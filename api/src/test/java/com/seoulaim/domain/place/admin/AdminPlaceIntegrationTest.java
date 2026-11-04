package com.seoulaim.domain.place.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.seoulaim.domain.artist.entity.Artist;
import com.seoulaim.domain.artist.entity.ArtistGroup;
import com.seoulaim.domain.artist.repository.ArtistGroupRepository;
import com.seoulaim.domain.artist.repository.ArtistRepository;
import com.seoulaim.domain.file.entity.File;
import com.seoulaim.domain.file.entity.FileDomain;
import com.seoulaim.domain.file.repository.FileRepository;
import com.seoulaim.domain.place.admin.dto.AdminPlaceDetailResponse;
import com.seoulaim.domain.place.admin.dto.AdminPlaceListRequest;
import com.seoulaim.domain.place.admin.dto.AdminPlaceListResponse;
import com.seoulaim.domain.place.admin.dto.AdminPlaceOperatingPeriod;
import com.seoulaim.domain.place.admin.dto.AdminPlaceRequest;
import com.seoulaim.domain.place.admin.dto.AdminPlaceRequest.AdminPlaceArtistRequest;
import com.seoulaim.domain.place.admin.dto.AdminPlaceRequest.AdminPlaceLocation;
import com.seoulaim.domain.place.admin.dto.AdminPlaceRequest.AdminPlaceOpeningHourRequest;
import com.seoulaim.domain.place.admin.dto.AdminPlaceSummaryResponse;
import com.seoulaim.domain.place.entity.Keyword;
import com.seoulaim.domain.place.entity.Place;
import com.seoulaim.domain.place.entity.PlaceCategory;
import com.seoulaim.domain.place.entity.Weekday;
import com.seoulaim.domain.place.repository.ArtistPlaceRepository;
import com.seoulaim.domain.place.repository.KeywordRepository;
import com.seoulaim.domain.place.repository.PlaceEventRepository;
import com.seoulaim.domain.place.repository.PlaceFavoriteRepository;
import com.seoulaim.domain.place.repository.PlaceImageRepository;
import com.seoulaim.domain.place.repository.PlaceKeywordRepository;
import com.seoulaim.domain.place.repository.PlaceOpeningHourRepository;
import com.seoulaim.domain.place.repository.PlaceRepository;
import com.seoulaim.domain.place.service.AdminPlaceService;
import com.seoulaim.domain.plan.entity.PlanDay;
import com.seoulaim.domain.plan.entity.PlanItem;
import com.seoulaim.domain.plan.entity.TravelPlan;
import com.seoulaim.domain.plan.repository.PlanDayRepository;
import com.seoulaim.domain.plan.repository.PlanItemRepository;
import com.seoulaim.domain.plan.repository.TravelPlanRepository;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.repository.UserRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@SpringBootTest
@Testcontainers
class AdminPlaceIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  private static final String IMAGE_1 = "place/2026/11/one.png";
  private static final String IMAGE_2 = "place/2026/11/two.png";

  @Autowired private AdminPlaceService adminPlaceService;
  @Autowired private PlaceRepository placeRepository;
  @Autowired private PlaceImageRepository placeImageRepository;
  @Autowired private PlaceEventRepository placeEventRepository;
  @Autowired private PlaceOpeningHourRepository placeOpeningHourRepository;
  @Autowired private PlaceKeywordRepository placeKeywordRepository;
  @Autowired private ArtistPlaceRepository artistPlaceRepository;
  @Autowired private PlaceFavoriteRepository placeFavoriteRepository;
  @Autowired private KeywordRepository keywordRepository;
  @Autowired private ArtistRepository artistRepository;
  @Autowired private ArtistGroupRepository artistGroupRepository;
  @Autowired private PlanItemRepository planItemRepository;
  @Autowired private PlanDayRepository planDayRepository;
  @Autowired private TravelPlanRepository travelPlanRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private FileRepository fileRepository;

  private ArtistGroup ateez;
  private Artist san;
  private ArtistGroup seventeen;

  @BeforeEach
  void setUp() {
    planItemRepository.deleteAllInBatch();
    planDayRepository.deleteAllInBatch();
    travelPlanRepository.deleteAllInBatch();
    placeFavoriteRepository.deleteAllInBatch();
    artistPlaceRepository.deleteAllInBatch();
    placeKeywordRepository.deleteAllInBatch();
    placeEventRepository.deleteAllInBatch();
    placeOpeningHourRepository.deleteAllInBatch();
    placeImageRepository.deleteAllInBatch();
    placeRepository.deleteAllInBatch();
    keywordRepository.deleteAllInBatch();
    artistRepository.deleteAllInBatch();
    artistGroupRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();
    fileRepository.deleteAllInBatch();

    keywordRepository.save(Keyword.create("CAFE", 0));
    keywordRepository.save(Keyword.create("PHOTO_SPOT", 1));
    ateez =
        artistGroupRepository.save(ArtistGroup.create("ATEEZ", "에이티즈", null, null, null, null, 1));
    san = artistRepository.save(Artist.create(ateez, "SAN", "산", null, null, null, null, 1));
    seventeen =
        artistGroupRepository.save(
            ArtistGroup.create("SEVENTEEN", "세븐틴", null, null, null, null, 2));
    fileRepository.save(File.createPending(null, FileDomain.PLACE, IMAGE_1, "image/png", 10L));
    fileRepository.save(File.createPending(null, FileDomain.PLACE, IMAGE_2, "image/png", 10L));
  }

  @Test
  @DisplayName("장소를 등록하면 사진·운영 기간·영업시간·키워드·멤버 태그가 함께 저장되고 사진 파일이 연결된다")
  void createPlace_전체저장() {
    AdminPlaceDetailResponse created =
        adminPlaceService.createPlace(
            request(
                "SAN 생일카페",
                true,
                List.of(IMAGE_1, IMAGE_2),
                new AdminPlaceOperatingPeriod(
                    LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 18)),
                dailyHours(),
                List.of("CAFE", "PHOTO_SPOT"),
                List.of(new AdminPlaceArtistRequest(ateez.getId(), san.getId(), "산 생일"))));

    assertThat(created.name()).isEqualTo("SAN 생일카페");
    assertThat(created.location().latitude()).isEqualTo(37.5512);
    assertThat(created.imageKeys()).containsExactly(IMAGE_1, IMAGE_2);
    assertThat(created.operatingPeriod().startsOn()).isEqualTo(LocalDate.of(2026, 11, 9));
    assertThat(created.openingHours()).hasSize(7);
    assertThat(created.openingHours().get(2).breakStartsAt()).isEqualTo(LocalTime.of(15, 0));
    assertThat(created.openingHours().get(6).closed()).isTrue();
    assertThat(created.keywordCodes()).containsExactly("CAFE", "PHOTO_SPOT");
    assertThat(created.artists()).hasSize(1);
    assertThat(created.artists().getFirst().artistNameEn()).isEqualTo("SAN");
    assertThat(created.updatedAt()).isNotNull();
    assertThat(fileRepository.findAll()).allMatch(File::isLinked);
  }

  @Test
  @DisplayName("장소를 수정하면 하위 데이터가 보낸 값으로 통째로 교체된다")
  void updatePlace_전체교체() {
    AdminPlaceDetailResponse created =
        adminPlaceService.createPlace(
            request(
                "SAN 생일카페",
                true,
                List.of(IMAGE_1),
                new AdminPlaceOperatingPeriod(
                    LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 18)),
                dailyHours(),
                List.of("CAFE"),
                List.of(new AdminPlaceArtistRequest(ateez.getId(), san.getId(), null))));

    AdminPlaceDetailResponse updated =
        adminPlaceService.updatePlace(
            created.id(),
            request(
                "어니언 성수",
                false,
                List.of(IMAGE_2, IMAGE_1),
                null,
                null,
                List.of("PHOTO_SPOT"),
                List.of(new AdminPlaceArtistRequest(seventeen.getId(), null, "그룹 연결"))));

    assertThat(updated.name()).isEqualTo("어니언 성수");
    assertThat(updated.visible()).isFalse();
    assertThat(updated.imageKeys()).containsExactly(IMAGE_2, IMAGE_1);
    assertThat(updated.operatingPeriod()).isNull();
    assertThat(updated.openingHours()).isNull();
    assertThat(updated.keywordCodes()).containsExactly("PHOTO_SPOT");
    assertThat(updated.artists()).hasSize(1);
    assertThat(updated.artists().getFirst().artistId()).isNull();
    assertThat(updated.artists().getFirst().artistGroupNameEn()).isEqualTo("SEVENTEEN");
    assertThat(placeEventRepository.count()).isZero();
    assertThat(placeOpeningHourRepository.count()).isZero();
  }

  @Test
  @DisplayName("목록은 노출 여부·그룹·태그 없음·이름으로 필터하고 전체 개수와 커서를 함께 반환한다")
  void getPlaces_필터와커서() {
    UUID cafeId =
        adminPlaceService
            .createPlace(
                request(
                    "어니언 성수",
                    true,
                    List.of(IMAGE_1),
                    null,
                    dailyHours(),
                    List.of("CAFE"),
                    List.of(new AdminPlaceArtistRequest(ateez.getId(), null, null))))
            .id();
    UUID hiddenId =
        adminPlaceService
            .createPlace(request("숨김 장소", false, List.of(), null, null, List.of(), List.of()))
            .id();
    UUID untaggedId =
        adminPlaceService
            .createPlace(request("태그 없는 장소", true, List.of(), null, null, List.of(), List.of()))
            .id();

    AdminPlaceListResponse all =
        adminPlaceService.getPlaces(listRequest(null, null, null, null, null, 2));
    assertThat(all.totalCount()).isEqualTo(3);
    assertThat(all.items())
        .extracting(AdminPlaceSummaryResponse::id)
        .containsExactly(untaggedId, hiddenId);
    AdminPlaceListResponse next =
        adminPlaceService.getPlaces(listRequest(null, null, null, null, all.nextCursor(), 2));
    assertThat(next.items()).extracting(AdminPlaceSummaryResponse::id).containsExactly(cafeId);
    assertThat(next.nextCursor()).isNull();

    AdminPlaceSummaryResponse cafe = next.items().getFirst();
    assertThat(cafe.thumbnailKey()).isEqualTo(IMAGE_1);
    assertThat(cafe.artistCount()).isEqualTo(1);
    assertThat(cafe.keywordCount()).isEqualTo(1);
    assertThat(cafe.openingHours()).hasSize(7);

    assertThat(ids(listRequest(null, false, null, null, null, 20))).containsExactly(hiddenId);
    assertThat(ids(listRequest(null, null, ateez.getId(), null, null, 20))).containsExactly(cafeId);
    assertThat(ids(listRequest(null, null, null, true, null, 20)))
        .containsExactlyInAnyOrder(hiddenId, untaggedId);
    assertThat(ids(listRequest("어니언", null, null, null, null, 20))).containsExactly(cafeId);
  }

  @Test
  @DisplayName("영업시간 요일이 7개가 아니거나 브레이크가 영업 구간 밖이면 PLACE_INVALID_OPENING_HOURS 예외가 발생한다")
  void createPlace_잘못된영업시간_예외() {
    List<AdminPlaceOpeningHourRequest> sixDays = dailyHours().subList(0, 6);
    // 22:00 ~ 다음 날 10:00 영업에 브레이크가 마감 뒤 (자정 넘김 자체는 허용)
    List<AdminPlaceOpeningHourRequest> breakOutside =
        Arrays.stream(Weekday.values())
            .map(
                day ->
                    new AdminPlaceOpeningHourRequest(
                        day,
                        false,
                        LocalTime.of(22, 0),
                        LocalTime.of(10, 0),
                        LocalTime.of(9, 30),
                        LocalTime.of(11, 0)))
            .toList();

    for (List<AdminPlaceOpeningHourRequest> hours : List.of(sixDays, breakOutside)) {
      assertThatThrownBy(
              () ->
                  adminPlaceService.createPlace(
                      request("장소", true, List.of(), null, hours, List.of(), List.of())))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.PLACE_INVALID_OPENING_HOURS);
    }
  }

  @Test
  @DisplayName("운영 기간 시작일이 종료일보다 늦으면 PLACE_INVALID_OPERATING_PERIOD 예외가 발생한다")
  void createPlace_잘못된운영기간_예외() {
    assertThatThrownBy(
            () ->
                adminPlaceService.createPlace(
                    request(
                        "장소",
                        true,
                        List.of(),
                        new AdminPlaceOperatingPeriod(
                            LocalDate.of(2026, 11, 18), LocalDate.of(2026, 11, 9)),
                        null,
                        List.of(),
                        List.of())))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PLACE_INVALID_OPERATING_PERIOD);
  }

  @Test
  @DisplayName("멤버가 다른 그룹 소속이면 ARTIST_NOT_IN_GROUP, 없는 키워드면 PLACE_KEYWORD_NOT_FOUND 예외가 발생한다")
  void createPlace_잘못된태그_예외() {
    assertThatThrownBy(
            () ->
                adminPlaceService.createPlace(
                    request(
                        "장소",
                        true,
                        List.of(),
                        null,
                        null,
                        List.of(),
                        List.of(
                            new AdminPlaceArtistRequest(seventeen.getId(), san.getId(), null)))))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ARTIST_NOT_IN_GROUP);

    assertThatThrownBy(
            () ->
                adminPlaceService.createPlace(
                    request("장소", true, List.of(), null, null, List.of("UNKNOWN"), List.of())))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PLACE_KEYWORD_NOT_FOUND);
  }

  @Test
  @DisplayName("일정에 포함된 장소는 PLACE_IN_USE 로 삭제를 막고, 아니면 하위 데이터까지 삭제한다")
  void deletePlace_일정포함여부() {
    UUID usedId =
        adminPlaceService
            .createPlace(
                request(
                    "일정 장소",
                    true,
                    List.of(IMAGE_1),
                    null,
                    dailyHours(),
                    List.of("CAFE"),
                    List.of()))
            .id();
    UUID freeId =
        adminPlaceService
            .createPlace(
                request("빈 장소", true, List.of(IMAGE_2), null, dailyHours(), List.of(), List.of()))
            .id();

    User user = userRepository.save(User.createWithEmail("luna@example.com", "hash", "Luna"));
    TravelPlan plan =
        travelPlanRepository.save(
            TravelPlan.create(user, LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 9)));
    PlanDay day = planDayRepository.save(PlanDay.create(plan, 1, LocalDate.of(2026, 11, 9)));
    Place usedPlace = placeRepository.findById(usedId).orElseThrow();
    planItemRepository.save(PlanItem.create(day, usedPlace, 1, LocalTime.of(10, 0)));

    assertThatThrownBy(() -> adminPlaceService.deletePlace(usedId))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PLACE_IN_USE);

    adminPlaceService.deletePlace(freeId);

    assertThat(placeRepository.existsById(freeId)).isFalse();
    assertThat(placeRepository.existsById(usedId)).isTrue();
    assertThat(placeOpeningHourRepository.count()).isEqualTo(7);
    assertThat(placeImageRepository.count()).isEqualTo(1);
  }

  private AdminPlaceRequest request(
      String name,
      boolean visible,
      List<String> imageKeys,
      AdminPlaceOperatingPeriod period,
      List<AdminPlaceOpeningHourRequest> hours,
      List<String> keywordCodes,
      List<AdminPlaceArtistRequest> artists) {
    return new AdminPlaceRequest(
        name,
        PlaceCategory.FANDOM_PLACE,
        visible,
        "서울 마포구 와우산로 21",
        "마포구",
        new AdminPlaceLocation(37.5512, 126.9227),
        60,
        null,
        null,
        imageKeys,
        period,
        hours,
        keywordCodes,
        artists);
  }

  // 월~토 11:00~20:00 (수요일 15:00~16:00 브레이크), 일요일 휴무
  private List<AdminPlaceOpeningHourRequest> dailyHours() {
    return Arrays.stream(Weekday.values())
        .map(
            day ->
                switch (day) {
                  case SUN -> new AdminPlaceOpeningHourRequest(day, true, null, null, null, null);
                  case WED ->
                      new AdminPlaceOpeningHourRequest(
                          day,
                          false,
                          LocalTime.of(11, 0),
                          LocalTime.of(20, 0),
                          LocalTime.of(15, 0),
                          LocalTime.of(16, 0));
                  default ->
                      new AdminPlaceOpeningHourRequest(
                          day, false, LocalTime.of(11, 0), LocalTime.of(20, 0), null, null);
                })
        .toList();
  }

  private AdminPlaceListRequest listRequest(
      String keyword,
      Boolean visible,
      UUID artistGroupId,
      Boolean untagged,
      String cursor,
      int size) {
    return new AdminPlaceListRequest(keyword, visible, null, artistGroupId, untagged, cursor, size);
  }

  private List<UUID> ids(AdminPlaceListRequest request) {
    return adminPlaceService.getPlaces(request).items().stream()
        .map(AdminPlaceSummaryResponse::id)
        .toList();
  }
}
