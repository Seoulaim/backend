package com.seoulaim.domain.content.controller;

import com.seoulaim.domain.content.dto.DiaryBannerResponse;
import com.seoulaim.domain.content.dto.FanScheduleResponse;
import com.seoulaim.domain.content.dto.HomeBirthdayCafeResponse;
import com.seoulaim.domain.content.dto.HomeEventResponse;
import com.seoulaim.domain.content.dto.HomePromoResponse;
import com.seoulaim.domain.content.service.HomeContentService;
import com.seoulaim.global.common.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 회원 앱 홈·다이어리 콘텐츠 (공개 GET). 토큰이 있으면 최애 그룹으로 거른다. */
@Tag(name = "Home")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class HomeContentController {

  private final HomeContentService homeContentService;

  @Operation(summary = "홈 메인 배너 (기본 배너 + 켜져 있고 기간 안인 배너, 어드민 순서)")
  @GetMapping("/home/promos")
  public List<HomePromoResponse> getHomePromos() {
    return homeContentService.getHomePromos();
  }

  @Operation(summary = "다이어리 배너 (홈 배너와 같은 규칙 + ctaLabel)")
  @GetMapping("/diary-banners")
  public List<DiaryBannerResponse> getDiaryBanners() {
    return homeContentService.getDiaryBanners();
  }

  @Operation(summary = "Event coming up (최애 그룹·all, 전용 항목이 없으면 전체. 최대 10)")
  @GetMapping("/home/events")
  public List<HomeEventResponse> getUpcomingEvents(
      @Parameter(hidden = true) @CurrentUserId(required = false) UUID userId) {
    return homeContentService.getUpcomingEvents(userId);
  }

  @Operation(summary = "생일카페 (생일 멤버의 그룹이 최애 그룹인 것, 없으면 전체. 최대 10)")
  @GetMapping("/home/birthday-cafes")
  public List<HomeBirthdayCafeResponse> getBirthdayCafes(
      @Parameter(hidden = true) @CurrentUserId(required = false) UUID userId) {
    return homeContentService.getBirthdayCafes(userId);
  }

  @Operation(summary = "팬캘린더 일정 (from~to 시작, 최애 그룹·all 만, 날짜순. 대체 없음)")
  @GetMapping("/home/fan-schedules")
  public List<FanScheduleResponse> getFanSchedules(
      @Parameter(hidden = true) @CurrentUserId(required = false) UUID userId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return homeContentService.getFanSchedules(userId, from, to);
  }
}
