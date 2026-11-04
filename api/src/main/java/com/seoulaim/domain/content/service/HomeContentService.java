package com.seoulaim.domain.content.service;

import com.seoulaim.domain.artist.service.ArtistService;
import com.seoulaim.domain.artist.service.UserBiasService;
import com.seoulaim.domain.content.dto.DiaryBannerResponse;
import com.seoulaim.domain.content.dto.FanScheduleResponse;
import com.seoulaim.domain.content.dto.HomeBirthdayCafeResponse;
import com.seoulaim.domain.content.dto.HomeEventResponse;
import com.seoulaim.domain.content.dto.HomePromoResponse;
import com.seoulaim.domain.content.entity.Banner;
import com.seoulaim.domain.content.entity.BannerPlacement;
import com.seoulaim.domain.content.entity.BirthdayCafe;
import com.seoulaim.domain.content.entity.CalendarEntry;
import com.seoulaim.domain.content.repository.BannerRepository;
import com.seoulaim.domain.content.repository.BirthdayCafeRepository;
import com.seoulaim.domain.content.repository.CalendarEntryRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 앱 홈(HH01)·다이어리(DD01) 콘텐츠 조회. 날짜 판단은 전부 KST 오늘.
 *
 * <p>userId 가 null 이면 비로그인 → 최애 필터 없음.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeContentService {

  static final ZoneId KST = ZoneId.of("Asia/Seoul");
  static final int UPCOMING_EVENT_LIMIT = 10;
  static final int BIRTHDAY_CAFE_LIMIT = 10;
  static final long MAX_FAN_SCHEDULE_DAYS = 366;

  private final BannerRepository bannerRepository;
  private final CalendarEntryRepository calendarEntryRepository;
  private final BirthdayCafeRepository birthdayCafeRepository;
  private final UserBiasService userBiasService;
  private final ArtistService artistService;
  private final Clock clock;

  public List<HomePromoResponse> getHomePromos() {
    return visibleBanners(BannerPlacement.HOME).stream().map(HomePromoResponse::from).toList();
  }

  public List<DiaryBannerResponse> getDiaryBanners() {
    return visibleBanners(BannerPlacement.DIARY).stream().map(DiaryBannerResponse::from).toList();
  }

  /** 사진 있고 끝나지 않은 항목. 최애 그룹 전용이 없으면 전체. 최대 10, 어드민 순서. */
  public List<HomeEventResponse> getUpcomingEvents(UUID userId) {
    List<CalendarEntry> events =
        calendarEntryRepository
            .findAllByImageKeyIsNotNullAndEndDateGreaterThanEqualOrderByDisplayOrderAscIdAsc(
                today());
    return BiasGroupFilter.filter(
            events,
            CalendarEntry::getArtistGroupIds,
            CalendarEntry::isAllGroups,
            biasGroupIds(userId),
            true)
        .stream()
        .limit(UPCOMING_EVENT_LIMIT)
        .map(HomeEventResponse::from)
        .toList();
  }

  /** 끝나지 않은 생일카페 중 생일 멤버의 그룹이 최애 그룹인 것. 없으면 전체. 최대 10, 어드민 순서. */
  public List<HomeBirthdayCafeResponse> getBirthdayCafes(UUID userId) {
    List<BirthdayCafe> cafes =
        birthdayCafeRepository.findAllByEndDateGreaterThanEqualOrderByDisplayOrderAscIdAsc(today());
    Set<UUID> biasGroupIds = biasGroupIds(userId);
    Map<UUID, UUID> groupIdByArtist =
        biasGroupIds.isEmpty()
            ? Map.of()
            : artistService.findGroupIdsByArtistIds(
                cafes.stream().map(BirthdayCafe::getArtistId).distinct().toList());
    return BiasGroupFilter.filter(
            cafes,
            cafe -> {
              UUID groupId = groupIdByArtist.get(cafe.getArtistId());
              return groupId == null ? Set.of() : Set.of(groupId);
            },
            cafe -> false,
            biasGroupIds,
            true)
        .stream()
        .limit(BIRTHDAY_CAFE_LIMIT)
        .map(HomeBirthdayCafeResponse::from)
        .toList();
  }

  /** from~to(포함) 에 시작하는 inCalendar 항목 중 최애 그룹·all 만. 대체 없음, 날짜순. */
  public List<FanScheduleResponse> getFanSchedules(UUID userId, LocalDate from, LocalDate to) {
    if (to.isBefore(from)) {
      throw LocalizedTexts.invalid("to", "from 보다 빠를 수 없습니다");
    }
    if (ChronoUnit.DAYS.between(from, to) >= MAX_FAN_SCHEDULE_DAYS) {
      throw LocalizedTexts.invalid("to", "조회 기간은 최대 " + MAX_FAN_SCHEDULE_DAYS + "일입니다");
    }
    List<CalendarEntry> entries =
        calendarEntryRepository
            .findAllByInCalendarTrueAndStartDateBetweenOrderByStartDateAscDisplayOrderAscIdAsc(
                from, to);
    return BiasGroupFilter.filter(
            entries,
            CalendarEntry::getArtistGroupIds,
            CalendarEntry::isAllGroups,
            biasGroupIds(userId),
            false)
        .stream()
        .map(FanScheduleResponse::from)
        .toList();
  }

  // 기본 배너(항상) + 켜져 있고 기간 안인 배너, 어드민 순서
  private List<Banner> visibleBanners(BannerPlacement placement) {
    LocalDate today = today();
    return bannerRepository.findAllByPlacementOrderByDisplayOrderAscIdAsc(placement).stream()
        .filter(banner -> banner.isVisibleOn(today))
        .toList();
  }

  private Set<UUID> biasGroupIds(UUID userId) {
    return userId == null ? Set.of() : userBiasService.findMyBiasGroupIds(userId);
  }

  private LocalDate today() {
    return LocalDate.ofInstant(clock.instant(), KST);
  }
}
