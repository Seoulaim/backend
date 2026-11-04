package com.seoulaim.domain.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.seoulaim.domain.artist.service.ArtistService;
import com.seoulaim.domain.artist.service.UserBiasService;
import com.seoulaim.domain.content.dto.FanScheduleResponse;
import com.seoulaim.domain.content.dto.HomeBirthdayCafeResponse;
import com.seoulaim.domain.content.dto.HomeEventResponse;
import com.seoulaim.domain.content.dto.HomePromoResponse;
import com.seoulaim.domain.content.entity.Banner;
import com.seoulaim.domain.content.entity.BannerPlacement;
import com.seoulaim.domain.content.entity.BannerTargetType;
import com.seoulaim.domain.content.entity.BirthdayCafe;
import com.seoulaim.domain.content.entity.CalendarColor;
import com.seoulaim.domain.content.entity.CalendarEntry;
import com.seoulaim.domain.content.repository.BannerRepository;
import com.seoulaim.domain.content.repository.BirthdayCafeRepository;
import com.seoulaim.domain.content.repository.CalendarEntryRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HomeContentServiceTest {

  // UTC 11-04 15:30 = KST 11-05 00:30 → 오늘은 KST 기준 11-05
  private static final Instant NOW = Instant.parse("2026-11-04T15:30:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 11, 5);
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID ATEEZ = UUID.randomUUID();
  private static final UUID SKZ = UUID.randomUUID();
  private static final UUID ILLIT = UUID.randomUUID();

  @Mock private BannerRepository bannerRepository;
  @Mock private CalendarEntryRepository calendarEntryRepository;
  @Mock private BirthdayCafeRepository birthdayCafeRepository;
  @Mock private UserBiasService userBiasService;
  @Mock private ArtistService artistService;

  private HomeContentService homeContentService;

  @BeforeEach
  void setUp() {
    homeContentService =
        new HomeContentService(
            bannerRepository,
            calendarEntryRepository,
            birthdayCafeRepository,
            userBiasService,
            artistService,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static CalendarEntry entry(String name, boolean allGroups, UUID... groupIds) {
    return CalendarEntry.create(
        Map.of("en", name),
        TODAY,
        TODAY.plusDays(1),
        true,
        CalendarColor.PINK,
        "content/2026/11/" + name + ".jpg",
        null,
        null,
        allGroups,
        List.of(groupIds),
        0);
  }

  private void givenEvents(CalendarEntry... entries) {
    given(
            calendarEntryRepository
                .findAllByImageKeyIsNotNullAndEndDateGreaterThanEqualOrderByDisplayOrderAscIdAsc(
                    TODAY))
        .willReturn(List.of(entries));
  }

  private static List<String> eventNames(List<HomeEventResponse> events) {
    return events.stream().map(event -> event.title().get("en")).toList();
  }

  @Test
  @DisplayName("최애 그룹 전용 이벤트가 있으면 전용 + all 항목만 반환한다")
  void getUpcomingEvents_전용항목있음_전용과all만반환() {
    givenEvents(entry("ateez", false, ATEEZ), entry("skz", false, SKZ), entry("festival", true));
    given(userBiasService.findMyBiasGroupIds(USER_ID)).willReturn(Set.of(ATEEZ));

    List<HomeEventResponse> events = homeContentService.getUpcomingEvents(USER_ID);

    assertThat(eventNames(events)).containsExactly("ateez", "festival");
    assertThat(events).allSatisfy(event -> assertThat(event.targetType()).isEqualTo("URL"));
  }

  @Test
  @DisplayName("all 항목만 최애와 맞으면 '없음'으로 보고 끝나지 않은 전체를 반환한다")
  void getUpcomingEvents_all만맞음_전체반환() {
    givenEvents(entry("ateez", false, ATEEZ), entry("festival", true));
    given(userBiasService.findMyBiasGroupIds(USER_ID)).willReturn(Set.of(ILLIT));

    assertThat(eventNames(homeContentService.getUpcomingEvents(USER_ID)))
        .containsExactly("ateez", "festival");
  }

  @Test
  @DisplayName("최애와 맞는 항목이 전혀 없으면 전체를 반환한다")
  void getUpcomingEvents_전부없음_전체반환() {
    givenEvents(entry("ateez", false, ATEEZ), entry("skz", false, SKZ));
    given(userBiasService.findMyBiasGroupIds(USER_ID)).willReturn(Set.of(ILLIT));

    assertThat(eventNames(homeContentService.getUpcomingEvents(USER_ID)))
        .containsExactly("ateez", "skz");
  }

  @Test
  @DisplayName("비로그인이면 bias 조회 없이 전체를 최대 10개까지 반환한다")
  void getUpcomingEvents_비로그인_전체최대10개() {
    givenEvents(
        IntStream.range(0, 12)
            .mapToObj(i -> entry("e" + i, false, ATEEZ))
            .toArray(CalendarEntry[]::new));

    List<HomeEventResponse> events = homeContentService.getUpcomingEvents(null);

    assertThat(events).hasSize(HomeContentService.UPCOMING_EVENT_LIMIT);
    verifyNoInteractions(userBiasService);
  }

  @Test
  @DisplayName("생일카페는 생일 멤버의 그룹이 최애 그룹인 것만, 없으면 전체를 반환한다")
  void getBirthdayCafes_멤버그룹기준_대체규칙() {
    UUID san = UUID.randomUUID();
    UUID felix = UUID.randomUUID();
    BirthdayCafe sanCafe =
        BirthdayCafe.create(san, Map.of("en", "A"), TODAY, TODAY.plusDays(2), null, null, 0);
    BirthdayCafe felixCafe =
        BirthdayCafe.create(felix, Map.of("en", "B"), TODAY, TODAY.plusDays(2), null, null, 1);
    given(birthdayCafeRepository.findAllByEndDateGreaterThanEqualOrderByDisplayOrderAscIdAsc(TODAY))
        .willReturn(List.of(sanCafe, felixCafe));
    given(artistService.findGroupIdsByArtistIds(any())).willReturn(Map.of(san, ATEEZ, felix, SKZ));

    given(userBiasService.findMyBiasGroupIds(USER_ID)).willReturn(Set.of(SKZ));
    assertThat(homeContentService.getBirthdayCafes(USER_ID))
        .extracting(HomeBirthdayCafeResponse::id)
        .containsExactly(felixCafe.getId());

    given(userBiasService.findMyBiasGroupIds(USER_ID)).willReturn(Set.of(ILLIT));
    assertThat(homeContentService.getBirthdayCafes(USER_ID))
        .extracting(HomeBirthdayCafeResponse::id)
        .containsExactly(sanCafe.getId(), felixCafe.getId());
  }

  @Test
  @DisplayName("팬캘린더는 대체가 없어 최애 전용 항목이 없으면 all 항목만, all 도 없으면 빈 목록이다")
  void getFanSchedules_대체없음() {
    LocalDate from = TODAY.withDayOfMonth(1);
    LocalDate to = from.plusMonths(1).minusDays(1);
    given(
            calendarEntryRepository
                .findAllByInCalendarTrueAndStartDateBetweenOrderByStartDateAscDisplayOrderAscIdAsc(
                    from, to))
        .willReturn(List.of(entry("ateez", false, ATEEZ), entry("festival", true)));
    given(userBiasService.findMyBiasGroupIds(USER_ID)).willReturn(Set.of(ILLIT));

    List<FanScheduleResponse> schedules = homeContentService.getFanSchedules(USER_ID, from, to);

    assertThat(schedules).extracting(s -> s.title().get("en")).containsExactly("festival");
    assertThat(schedules.getFirst().date()).isEqualTo(TODAY);
    assertThat(schedules.getFirst().color()).isEqualTo(CalendarColor.PINK);

    given(
            calendarEntryRepository
                .findAllByInCalendarTrueAndStartDateBetweenOrderByStartDateAscDisplayOrderAscIdAsc(
                    from, to))
        .willReturn(List.of(entry("ateez", false, ATEEZ)));
    assertThat(homeContentService.getFanSchedules(USER_ID, from, to)).isEmpty();
  }

  @Test
  @DisplayName("팬캘린더 to 가 from 보다 빠르면 COMMON_INVALID_REQUEST")
  void getFanSchedules_기간역전_400() {
    assertThatThrownBy(() -> homeContentService.getFanSchedules(null, TODAY, TODAY.minusDays(1)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);
  }

  private static Banner banner(String name, LocalDate start, LocalDate end) {
    return Banner.create(
        BannerPlacement.HOME,
        name,
        null,
        Map.of("en", name),
        null,
        null,
        BannerTargetType.DIARY,
        null,
        start,
        end,
        0);
  }

  @Test
  @DisplayName("홈 배너는 기본 배너를 항상 포함하고, 켜져 있고 KST 오늘이 기간 안(종료일 포함)인 것만 순서대로 반환한다")
  void getHomePromos_기본배너와기간안배너만() {
    Banner defaultBanner = banner("default", null, null);
    ReflectionTestUtils.setField(defaultBanner, "isDefault", true);
    Banner endsToday = banner("endsToday", TODAY.minusDays(3), TODAY);
    Banner endedYesterdayKst = banner("endedYesterday", TODAY.minusDays(3), TODAY.minusDays(1));
    Banner startsTomorrow = banner("startsTomorrow", TODAY.plusDays(1), TODAY.plusDays(5));
    Banner disabled = banner("disabled", TODAY.minusDays(1), TODAY.plusDays(1));
    disabled.changeEnabled(false);
    Banner openEnded = banner("openEnded", null, null);
    given(bannerRepository.findAllByPlacementOrderByDisplayOrderAscIdAsc(BannerPlacement.HOME))
        .willReturn(
            List.of(
                endsToday, defaultBanner, endedYesterdayKst, startsTomorrow, disabled, openEnded));

    assertThat(homeContentService.getHomePromos())
        .extracting(HomePromoResponse::title)
        .extracting(title -> title.get("en"))
        .containsExactly("endsToday", "default", "openEnded");
  }
}
