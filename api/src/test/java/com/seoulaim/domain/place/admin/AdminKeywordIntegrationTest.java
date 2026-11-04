package com.seoulaim.domain.place.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.seoulaim.domain.place.admin.dto.AdminKeywordListResponse;
import com.seoulaim.domain.place.admin.dto.AdminKeywordOrderRequest;
import com.seoulaim.domain.place.admin.dto.AdminKeywordRequest;
import com.seoulaim.domain.place.admin.dto.AdminKeywordRequest.AdminKeywordName;
import com.seoulaim.domain.place.admin.dto.AdminKeywordResponse;
import com.seoulaim.domain.place.entity.Keyword;
import com.seoulaim.domain.place.entity.Place;
import com.seoulaim.domain.place.entity.PlaceCategory;
import com.seoulaim.domain.place.entity.PlaceKeyword;
import com.seoulaim.domain.place.repository.ArtistPlaceRepository;
import com.seoulaim.domain.place.repository.KeywordRepository;
import com.seoulaim.domain.place.repository.PlaceEventRepository;
import com.seoulaim.domain.place.repository.PlaceFavoriteRepository;
import com.seoulaim.domain.place.repository.PlaceImageRepository;
import com.seoulaim.domain.place.repository.PlaceKeywordRepository;
import com.seoulaim.domain.place.repository.PlaceOpeningHourRepository;
import com.seoulaim.domain.place.repository.PlaceRepository;
import com.seoulaim.domain.place.service.AdminKeywordService;
import com.seoulaim.domain.plan.repository.PlanDayRepository;
import com.seoulaim.domain.plan.repository.PlanItemRepository;
import com.seoulaim.domain.plan.repository.TravelPlanRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.List;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@SpringBootTest
@Testcontainers
class AdminKeywordIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  private static final GeometryFactory GEOMETRY = new GeometryFactory(new PrecisionModel(), 4326);

  @Autowired private AdminKeywordService adminKeywordService;
  @Autowired private KeywordRepository keywordRepository;
  @Autowired private PlaceKeywordRepository placeKeywordRepository;
  @Autowired private PlaceRepository placeRepository;
  @Autowired private PlaceImageRepository placeImageRepository;
  @Autowired private PlaceEventRepository placeEventRepository;
  @Autowired private PlaceOpeningHourRepository placeOpeningHourRepository;
  @Autowired private ArtistPlaceRepository artistPlaceRepository;
  @Autowired private PlaceFavoriteRepository placeFavoriteRepository;
  @Autowired private PlanItemRepository planItemRepository;
  @Autowired private PlanDayRepository planDayRepository;
  @Autowired private TravelPlanRepository travelPlanRepository;

  private Keyword cafe;
  private Keyword photoSpot;

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

    cafe = keywordRepository.save(Keyword.create("CAFE", 0));
    photoSpot = keywordRepository.save(Keyword.create("PHOTO_SPOT", 1));
    Place place =
        placeRepository.save(
            Place.create(
                "Onion",
                PlaceCategory.FANDOM_PLACE,
                "서울 성동구 아차산로9길 8",
                GEOMETRY.createPoint(new Coordinate(127.0581, 37.5447)),
                "성동구",
                null,
                null));
    placeKeywordRepository.save(PlaceKeyword.create(place, cafe));
  }

  @Test
  @DisplayName("목록은 노출 순서대로 장소 수와 함께 조회된다")
  void getKeywords_장소수() {
    AdminKeywordListResponse response = adminKeywordService.getKeywords();

    assertThat(response.items())
        .extracting(AdminKeywordResponse::code)
        .containsExactly("CAFE", "PHOTO_SPOT");
    assertThat(response.items())
        .extracting(AdminKeywordResponse::placeCount)
        .containsExactly(1L, 0L);
  }

  @Test
  @DisplayName("code 를 생략하면 영어 이름으로 코드를 만들고 맨 뒤 순서로 추가된다")
  void createKeyword_코드생성() {
    AdminKeywordResponse created =
        adminKeywordService.createKeyword(
            new AdminKeywordRequest(null, new AdminKeywordName("Night View", "야경", "", null)));

    assertThat(created.code()).isEqualTo("NIGHT_VIEW");
    assertThat(created.displayOrder()).isEqualTo(2);
    assertThat(created.name().ko()).isEqualTo("야경");
    assertThat(created.name().ja()).isNull();
  }

  @Test
  @DisplayName("이미 있는 코드로 추가하면 PLACE_KEYWORD_DUPLICATED")
  void createKeyword_중복() {
    assertThatThrownBy(
            () ->
                adminKeywordService.createKeyword(
                    new AdminKeywordRequest(null, new AdminKeywordName("Cafe", null, null, null))))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PLACE_KEYWORD_DUPLICATED);
  }

  @Test
  @DisplayName("수정하면 이름만 바뀌고 코드는 유지된다")
  void updateKeyword_이름만() {
    AdminKeywordResponse updated =
        adminKeywordService.updateKeyword(
            cafe.getId(),
            new AdminKeywordRequest("OTHER", new AdminKeywordName("Cafe", "카페", "カフェ", "咖啡")));

    assertThat(updated.code()).isEqualTo("CAFE");
    assertThat(updated.name().ja()).isEqualTo("カフェ");
    assertThat(updated.placeCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("순서 변경은 전체 id 를 받아 0부터 다시 부여하고, 빠진 id 가 있으면 PLACE_KEYWORD_INVALID_ORDER")
  void reorderKeywords() {
    AdminKeywordListResponse response =
        adminKeywordService.reorderKeywords(
            new AdminKeywordOrderRequest(List.of(photoSpot.getId(), cafe.getId())));

    assertThat(response.items())
        .extracting(AdminKeywordResponse::code)
        .containsExactly("PHOTO_SPOT", "CAFE");
    assertThat(response.items())
        .extracting(AdminKeywordResponse::displayOrder)
        .containsExactly(0, 1);

    assertThatThrownBy(
            () ->
                adminKeywordService.reorderKeywords(
                    new AdminKeywordOrderRequest(List.of(cafe.getId()))))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PLACE_KEYWORD_INVALID_ORDER);
  }

  @Test
  @DisplayName("삭제하면 장소 연결도 함께 삭제된다")
  void deleteKeyword_연결삭제() {
    adminKeywordService.deleteKeyword(cafe.getId());

    assertThat(keywordRepository.findById(cafe.getId())).isEmpty();
    assertThat(placeKeywordRepository.count()).isZero();
  }
}
