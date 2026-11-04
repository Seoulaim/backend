package com.seoulaim.domain.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.content.admin.dto.AdminBannerReorderRequest;
import com.seoulaim.domain.content.admin.dto.AdminBannerRequest;
import com.seoulaim.domain.content.admin.dto.AdminBannerResponse;
import com.seoulaim.domain.content.entity.Banner;
import com.seoulaim.domain.content.entity.BannerPlacement;
import com.seoulaim.domain.content.entity.BannerTargetType;
import com.seoulaim.domain.content.repository.BannerRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminBannerServiceTest {

  private static final LocalDate START = LocalDate.of(2026, 11, 1);
  private static final LocalDate END = LocalDate.of(2026, 11, 30);

  @Mock private BannerRepository bannerRepository;
  @Mock private ContentImageResolver contentImageResolver;

  private AdminBannerService adminBannerService;

  @BeforeEach
  void setUp() {
    adminBannerService = new AdminBannerService(bannerRepository, contentImageResolver);
  }

  private static Banner banner(boolean isDefault) {
    Banner banner =
        Banner.create(
            BannerPlacement.HOME,
            "title",
            "content/2026/11/a.jpg",
            Map.of("en", "Seoulaim"),
            null,
            null,
            BannerTargetType.DIARY,
            null,
            null,
            null,
            0);
    ReflectionTestUtils.setField(banner, "isDefault", isDefault);
    return banner;
  }

  private static AdminBannerRequest request(
      BannerPlacement placement, BannerTargetType targetType, String targetUrl) {
    return new AdminBannerRequest(
        placement,
        "ATEEZ 팝업",
        null,
        null,
        Map.of("en", "ANITEEZ", "ko", " "),
        Map.of("en", "Pop-up"),
        null,
        targetType,
        targetUrl,
        START,
        END);
  }

  @Test
  @DisplayName("기본 배너를 끄면 CONTENT_DEFAULT_BANNER")
  void changeEnabled_기본배너_409() {
    Banner defaultBanner = banner(true);
    given(bannerRepository.findById(defaultBanner.getId())).willReturn(Optional.of(defaultBanner));

    assertThatThrownBy(() -> adminBannerService.changeEnabled(defaultBanner.getId(), false))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CONTENT_DEFAULT_BANNER);
    assertThat(defaultBanner.isEnabled()).isTrue();
  }

  @Test
  @DisplayName("기본 배너를 삭제하면 CONTENT_DEFAULT_BANNER 이고 삭제하지 않는다")
  void deleteBanner_기본배너_409() {
    Banner defaultBanner = banner(true);
    given(bannerRepository.findById(defaultBanner.getId())).willReturn(Optional.of(defaultBanner));

    assertThatThrownBy(() -> adminBannerService.deleteBanner(defaultBanner.getId()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CONTENT_DEFAULT_BANNER);
    verify(bannerRepository, never()).delete(any());
  }

  @Test
  @DisplayName("일반 배너는 끌 수 있다")
  void changeEnabled_일반배너_꺼짐() {
    Banner normal = banner(false);
    given(bannerRepository.findById(normal.getId())).willReturn(Optional.of(normal));

    assertThat(adminBannerService.changeEnabled(normal.getId(), false).enabled()).isFalse();
  }

  @Test
  @DisplayName("기본 배너를 수정하면 기간은 null 로 고정되고 위치는 바뀌지 않으며 이미지는 유지된다")
  void updateBanner_기본배너_기간null위치유지() {
    Banner defaultBanner = banner(true);
    given(bannerRepository.findById(defaultBanner.getId())).willReturn(Optional.of(defaultBanner));
    given(contentImageResolver.resolve("content/2026/11/a.jpg", null, null))
        .willReturn("content/2026/11/a.jpg");

    AdminBannerResponse response =
        adminBannerService.updateBanner(
            defaultBanner.getId(), request(BannerPlacement.DIARY, BannerTargetType.DIARY, null));

    assertThat(response.placement()).isEqualTo(BannerPlacement.HOME);
    assertThat(response.startDate()).isNull();
    assertThat(response.endDate()).isNull();
    assertThat(response.imageKey()).isEqualTo("content/2026/11/a.jpg");
    assertThat(response.title()).containsExactly(Map.entry("en", "ANITEEZ"));
  }

  @Test
  @DisplayName("새 배너는 켜진 상태로 그 위치 맨 뒤 순서에 추가된다")
  void createBanner_맨뒤에켜진상태() {
    given(bannerRepository.findMaxDisplayOrder(BannerPlacement.HOME)).willReturn(3);
    given(bannerRepository.save(any(Banner.class))).willAnswer(inv -> inv.getArgument(0));

    AdminBannerResponse response =
        adminBannerService.createBanner(
            request(BannerPlacement.HOME, BannerTargetType.URL, "https://example.com"));

    assertThat(response.displayOrder()).isEqualTo(4);
    assertThat(response.enabled()).isTrue();
    assertThat(response.isDefault()).isFalse();
    assertThat(response.startDate()).isEqualTo(START);
  }

  @Test
  @DisplayName("targetType 이 URL 인데 targetUrl 이 없으면 COMMON_INVALID_REQUEST")
  void createBanner_URL인데주소없음_400() {
    assertThatThrownBy(
            () ->
                adminBannerService.createBanner(
                    request(BannerPlacement.HOME, BannerTargetType.URL, " ")))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);
  }

  @Test
  @DisplayName("순서 변경은 그 위치의 배너가 하나라도 빠지면 400, 전부 보내면 순서대로 displayOrder 를 매긴다")
  void reorder_전체목록검증() {
    Banner first = banner(true);
    Banner second = banner(false);
    given(bannerRepository.findAllByPlacementOrderByDisplayOrderAscIdAsc(BannerPlacement.HOME))
        .willReturn(List.of(first, second));

    assertThatThrownBy(
            () ->
                adminBannerService.reorder(
                    new AdminBannerReorderRequest(BannerPlacement.HOME, List.of(second.getId()))))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);

    List<AdminBannerResponse> reordered =
        adminBannerService.reorder(
            new AdminBannerReorderRequest(
                BannerPlacement.HOME, List.of(second.getId(), first.getId())));

    assertThat(reordered)
        .extracting(AdminBannerResponse::id)
        .containsExactly(second.getId(), first.getId());
    assertThat(second.getDisplayOrder()).isZero();
    assertThat(first.getDisplayOrder()).isEqualTo(1);
  }

  @Test
  @DisplayName("없는 배너면 CONTENT_NOT_FOUND")
  void getBanner_없음_404() {
    UUID id = UUID.randomUUID();
    given(bannerRepository.findById(id)).willReturn(Optional.empty());

    assertThatThrownBy(() -> adminBannerService.getBanner(id))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.CONTENT_NOT_FOUND);
  }
}
