package com.seoulaim.domain.content.admin.controller;

import com.seoulaim.domain.content.admin.dto.AdminBannerEnabledRequest;
import com.seoulaim.domain.content.admin.dto.AdminBannerReorderRequest;
import com.seoulaim.domain.content.admin.dto.AdminBannerRequest;
import com.seoulaim.domain.content.admin.dto.AdminBannerResponse;
import com.seoulaim.domain.content.admin.dto.AdminBirthdayCafeRequest;
import com.seoulaim.domain.content.admin.dto.AdminBirthdayCafeResponse;
import com.seoulaim.domain.content.admin.dto.AdminCalendarEntryRequest;
import com.seoulaim.domain.content.admin.dto.AdminCalendarEntryResponse;
import com.seoulaim.domain.content.entity.BannerPlacement;
import com.seoulaim.domain.content.service.AdminBannerService;
import com.seoulaim.domain.content.service.AdminBirthdayCafeService;
import com.seoulaim.domain.content.service.AdminCalendarEntryService;
import com.seoulaim.global.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 어드민 이벤트 관리(팬캘린더·Event coming up·생일카페) + 프로모션 관리(배너). */
@Tag(name = "Admin")
@SecurityRequirement(name = SwaggerConfig.ADMIN_KEY_SCHEME)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminContentController {

  private final AdminCalendarEntryService adminCalendarEntryService;
  private final AdminBirthdayCafeService adminBirthdayCafeService;
  private final AdminBannerService adminBannerService;

  // ── 팬캘린더 · Event coming up ──

  @Operation(summary = "[관리자] 캘린더 항목 목록 (전체, 어드민 순서)")
  @GetMapping("/calendar-entries")
  public List<AdminCalendarEntryResponse> getCalendarEntries() {
    return adminCalendarEntryService.getEntries();
  }

  @Operation(summary = "[관리자] 캘린더 항목 상세")
  @GetMapping("/calendar-entries/{entryId}")
  public AdminCalendarEntryResponse getCalendarEntry(@PathVariable UUID entryId) {
    return adminCalendarEntryService.getEntry(entryId);
  }

  @Operation(summary = "[관리자] 캘린더 항목 등록 (맨 뒤에 추가)")
  @PostMapping("/calendar-entries")
  @ResponseStatus(HttpStatus.CREATED)
  public AdminCalendarEntryResponse createCalendarEntry(
      @Valid @RequestBody AdminCalendarEntryRequest request) {
    return adminCalendarEntryService.createEntry(request);
  }

  @Operation(summary = "[관리자] 캘린더 항목 수정 (imageFileId 생략 시 기존 사진 유지)")
  @PutMapping("/calendar-entries/{entryId}")
  public AdminCalendarEntryResponse updateCalendarEntry(
      @PathVariable UUID entryId, @Valid @RequestBody AdminCalendarEntryRequest request) {
    return adminCalendarEntryService.updateEntry(entryId, request);
  }

  @Operation(summary = "[관리자] 캘린더 항목 삭제")
  @DeleteMapping("/calendar-entries/{entryId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteCalendarEntry(@PathVariable UUID entryId) {
    adminCalendarEntryService.deleteEntry(entryId);
  }

  // ── 생일카페 ──

  @Operation(summary = "[관리자] 생일카페 목록 (전체, 어드민 순서)")
  @GetMapping("/birthday-cafes")
  public List<AdminBirthdayCafeResponse> getBirthdayCafes() {
    return adminBirthdayCafeService.getCafes();
  }

  @Operation(summary = "[관리자] 생일카페 상세")
  @GetMapping("/birthday-cafes/{cafeId}")
  public AdminBirthdayCafeResponse getBirthdayCafe(@PathVariable UUID cafeId) {
    return adminBirthdayCafeService.getCafe(cafeId);
  }

  @Operation(summary = "[관리자] 생일카페 등록 (맨 뒤에 추가)")
  @PostMapping("/birthday-cafes")
  @ResponseStatus(HttpStatus.CREATED)
  public AdminBirthdayCafeResponse createBirthdayCafe(
      @Valid @RequestBody AdminBirthdayCafeRequest request) {
    return adminBirthdayCafeService.createCafe(request);
  }

  @Operation(summary = "[관리자] 생일카페 수정 (imageFileId 생략 시 기존 사진 유지)")
  @PutMapping("/birthday-cafes/{cafeId}")
  public AdminBirthdayCafeResponse updateBirthdayCafe(
      @PathVariable UUID cafeId, @Valid @RequestBody AdminBirthdayCafeRequest request) {
    return adminBirthdayCafeService.updateCafe(cafeId, request);
  }

  @Operation(summary = "[관리자] 생일카페 삭제")
  @DeleteMapping("/birthday-cafes/{cafeId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteBirthdayCafe(@PathVariable UUID cafeId) {
    adminBirthdayCafeService.deleteCafe(cafeId);
  }

  // ── 배너 ──

  @Operation(summary = "[관리자] 배너 목록 (placement 지정: 그 위치의 어드민 순서, 생략: 전체 최신순)")
  @GetMapping("/banners")
  public List<AdminBannerResponse> getBanners(
      @RequestParam(required = false) BannerPlacement placement) {
    return adminBannerService.getBanners(placement);
  }

  @Operation(summary = "[관리자] 배너 상세")
  @GetMapping("/banners/{bannerId}")
  public AdminBannerResponse getBanner(@PathVariable UUID bannerId) {
    return adminBannerService.getBanner(bannerId);
  }

  @Operation(summary = "[관리자] 배너 등록 (켜진 상태로 그 위치 맨 뒤에 추가)")
  @PostMapping("/banners")
  @ResponseStatus(HttpStatus.CREATED)
  public AdminBannerResponse createBanner(@Valid @RequestBody AdminBannerRequest request) {
    return adminBannerService.createBanner(request);
  }

  @Operation(summary = "[관리자] 배너 순서 변경 (그 위치의 배너 전부를 새 순서로)")
  @PutMapping("/banners/reorder")
  public List<AdminBannerResponse> reorderBanners(
      @Valid @RequestBody AdminBannerReorderRequest request) {
    return adminBannerService.reorder(request);
  }

  @Operation(summary = "[관리자] 배너 수정 (placement 변경 불가, 기본 배너는 기간 없음)")
  @PutMapping("/banners/{bannerId}")
  public AdminBannerResponse updateBanner(
      @PathVariable UUID bannerId, @Valid @RequestBody AdminBannerRequest request) {
    return adminBannerService.updateBanner(bannerId, request);
  }

  @Operation(summary = "[관리자] 배너 켜기/끄기 (기본 배너는 409)")
  @PatchMapping("/banners/{bannerId}/enabled")
  public AdminBannerResponse changeBannerEnabled(
      @PathVariable UUID bannerId, @Valid @RequestBody AdminBannerEnabledRequest request) {
    return adminBannerService.changeEnabled(bannerId, request.enabled());
  }

  @Operation(summary = "[관리자] 배너 삭제 (기본 배너는 409)")
  @DeleteMapping("/banners/{bannerId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteBanner(@PathVariable UUID bannerId) {
    adminBannerService.deleteBanner(bannerId);
  }
}
