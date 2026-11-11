package com.seoulaim.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.seoulaim.domain.artist.repository.ArtistGroupRepository;
import com.seoulaim.domain.artist.repository.ArtistRepository;
import com.seoulaim.domain.file.service.FileService;
import com.seoulaim.domain.place.admin.dto.AdminPlaceDetailResponse;
import com.seoulaim.domain.place.entity.Place;
import com.seoulaim.domain.place.entity.PlaceCategory;
import com.seoulaim.domain.place.repository.ArtistPlaceRepository;
import com.seoulaim.domain.place.repository.KeywordRepository;
import com.seoulaim.domain.place.repository.PlaceEventRepository;
import com.seoulaim.domain.place.repository.PlaceFavoriteRepository;
import com.seoulaim.domain.place.repository.PlaceImageRepository;
import com.seoulaim.domain.place.repository.PlaceKeywordRepository;
import com.seoulaim.domain.place.repository.PlaceOpeningHourRepository;
import com.seoulaim.domain.place.repository.PlaceRepository;
import com.seoulaim.domain.plan.repository.PlanItemRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminPlaceServiceTest {

  private static final GeometryFactory GEOMETRY = new GeometryFactory(new PrecisionModel(), 4326);

  @Mock private PlaceRepository placeRepository;
  @Mock private PlaceImageRepository placeImageRepository;
  @Mock private PlaceEventRepository placeEventRepository;
  @Mock private PlaceOpeningHourRepository placeOpeningHourRepository;
  @Mock private PlaceKeywordRepository placeKeywordRepository;
  @Mock private ArtistPlaceRepository artistPlaceRepository;
  @Mock private PlaceFavoriteRepository placeFavoriteRepository;
  @Mock private KeywordRepository keywordRepository;
  @Mock private ArtistGroupRepository artistGroupRepository;
  @Mock private ArtistRepository artistRepository;
  @Mock private PlanItemRepository planItemRepository;
  @Mock private FileService fileService;

  @InjectMocks private AdminPlaceService adminPlaceService;

  @Test
  @DisplayName("영어 요약·주소가 비어 있어도 노출 여부만 변경된다")
  void changeVisibility_englishFieldsEmpty_succeeds() {
    // 한국어만 있는 장소: summary.en·address.en 이 null (전체 PUT 이면 검증 실패)
    Place place =
        Place.create(
            "카페",
            PlaceCategory.FANDOM_PLACE,
            "서울특별시 마포구",
            GEOMETRY.createPoint(new Coordinate(126.92, 37.55)),
            "마포구",
            null,
            null);
    given(placeRepository.findById(place.getId())).willReturn(Optional.of(place));

    AdminPlaceDetailResponse response = adminPlaceService.changeVisibility(place.getId(), false);

    assertThat(place.isVisible()).isFalse();
    assertThat(response.visible()).isFalse();
    assertThat(place.getSummaries().en()).isNull();
    assertThat(place.getAddresses().en()).isNull();
  }
}
