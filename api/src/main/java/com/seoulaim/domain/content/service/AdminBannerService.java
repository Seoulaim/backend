package com.seoulaim.domain.content.service;

import com.seoulaim.domain.content.admin.dto.AdminBannerReorderRequest;
import com.seoulaim.domain.content.admin.dto.AdminBannerRequest;
import com.seoulaim.domain.content.admin.dto.AdminBannerResponse;
import com.seoulaim.domain.content.entity.Banner;
import com.seoulaim.domain.content.entity.BannerPlacement;
import com.seoulaim.domain.content.entity.BannerTargetType;
import com.seoulaim.domain.content.repository.BannerRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 어드민 프로모션 관리: 홈·다이어리 배너
 *
 * <ul>
 *   <li>기본 배너는 위치별 1개 (처음엔 마이그레이션). 수정은 되지만 기간은 null 고정, 끄기·삭제는 409
 *   <li>기본 배너 전환: 이전 기본 배너는 해제만(enabled·기간 유지), 새 기본 배너는 켜고 기간을 지운다. 해제된 배너는 끄기·삭제 가능
 *   <li>새 배너는 켜진 상태로 그 위치 맨 뒤에 붙는다. isDefault=true 거나 위치에 배너가 없으면 기본 배너
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBannerService {

  private final BannerRepository bannerRepository;
  private final ContentImageResolver contentImageResolver;

  /** placement null: 전체 최신순. 지정: 그 위치의 어드민 순서. */
  public List<AdminBannerResponse> getBanners(BannerPlacement placement) {
    List<Banner> banners =
        placement == null
            ? bannerRepository.findAllByOrderByCreatedAtDescIdDesc()
            : bannerRepository.findAllByPlacementOrderByDisplayOrderAscIdAsc(placement);
    return banners.stream().map(AdminBannerResponse::from).toList();
  }

  public AdminBannerResponse getBanner(UUID bannerId) {
    return AdminBannerResponse.from(findBanner(bannerId));
  }

  @Transactional
  public AdminBannerResponse createBanner(AdminBannerRequest request) {
    if (request.placement() == null) {
      throw LocalizedTexts.invalid("placement", "HOME 또는 DIARY 가 필요합니다");
    }
    List<Banner> siblings = bannerRepository.findAllByPlacementForUpdate(request.placement());
    boolean asDefault = Boolean.TRUE.equals(request.isDefault()) || siblings.isEmpty();
    Texts texts = validate(request, asDefault);
    String imageKey =
        contentImageResolver.resolve(null, request.imageFileId(), request.removeImage());
    Banner banner =
        Banner.create(
            request.placement(),
            request.adminTitle().strip(),
            imageKey,
            texts.title(),
            texts.description(),
            texts.ctaLabel(),
            request.targetType(),
            texts.targetUrl(),
            request.startDate(),
            request.endDate(),
            bannerRepository.findMaxDisplayOrder(request.placement()) + 1);
    if (asDefault) {
      releaseCurrentDefault(siblings);
      banner.markDefault();
    }
    return AdminBannerResponse.from(bannerRepository.save(banner));
  }

  /** 그 배너를 위치의 기본 배너로. 이미 기본이면 그대로. 응답은 그 위치의 배너 목록(어드민 순서). */
  @Transactional
  public List<AdminBannerResponse> changeDefault(UUID bannerId) {
    BannerPlacement placement =
        bannerRepository
            .findPlacementById(bannerId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
    // 잠금 조회로 최신 상태를 읽는다 (대상 배너도 여기서 처음 로드)
    List<Banner> banners = bannerRepository.findAllByPlacementForUpdate(placement);
    Banner target =
        banners.stream()
            .filter(banner -> banner.getId().equals(bannerId))
            .findFirst()
            .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
    if (!target.isDefault()) {
      releaseCurrentDefault(banners);
      target.markDefault();
    }
    return banners.stream().map(AdminBannerResponse::from).toList();
  }

  // 이전 기본 배너 해제를 먼저 DB 에 반영한다 (uk_banners_default_placement 는 지연 검사가 아님)
  private void releaseCurrentDefault(List<Banner> banners) {
    banners.stream()
        .filter(Banner::isDefault)
        .forEach(
            current -> {
              current.unmarkDefault();
              bannerRepository.flush();
            });
  }

  /** placement 는 무시한다 (바꿀 수 없음). 기본 배너는 기간이 null 로 고정된다. */
  @Transactional
  public AdminBannerResponse updateBanner(UUID bannerId, AdminBannerRequest request) {
    Banner banner = findBanner(bannerId);
    Texts texts = validate(request, banner.isDefault());
    banner.update(
        request.adminTitle().strip(),
        contentImageResolver.resolve(
            banner.getImageKey(), request.imageFileId(), request.removeImage()),
        texts.title(),
        texts.description(),
        texts.ctaLabel(),
        request.targetType(),
        texts.targetUrl(),
        request.startDate(),
        request.endDate());
    return AdminBannerResponse.from(banner);
  }

  @Transactional
  public AdminBannerResponse changeEnabled(UUID bannerId, boolean enabled) {
    Banner banner = findEditableBanner(bannerId);
    banner.changeEnabled(enabled);
    return AdminBannerResponse.from(banner);
  }

  @Transactional
  public void deleteBanner(UUID bannerId) {
    bannerRepository.delete(findEditableBanner(bannerId));
  }

  /** ids 는 그 위치의 배너 전부(기본 배너 포함)여야 한다. 빠지거나 남거나 중복이면 400. */
  @Transactional
  public List<AdminBannerResponse> reorder(AdminBannerReorderRequest request) {
    List<Banner> banners =
        bannerRepository.findAllByPlacementOrderByDisplayOrderAscIdAsc(request.placement());
    Map<UUID, Banner> byId =
        banners.stream().collect(Collectors.toMap(Banner::getId, Function.identity()));
    Set<UUID> requested = new HashSet<>(request.ids());
    if (requested.size() != request.ids().size() || !requested.equals(byId.keySet())) {
      throw LocalizedTexts.invalid("ids", "그 위치의 배너 id 를 중복 없이 모두 보내야 합니다");
    }
    for (int index = 0; index < request.ids().size(); index++) {
      byId.get(request.ids().get(index)).changeDisplayOrder(index);
    }
    return request.ids().stream().map(byId::get).map(AdminBannerResponse::from).toList();
  }

  private Banner findBanner(UUID bannerId) {
    return bannerRepository
        .findById(bannerId)
        .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
  }

  // 끄기·삭제 대상. 기본 배너면 CONTENT_DEFAULT_BANNER
  private Banner findEditableBanner(UUID bannerId) {
    Banner banner = findBanner(bannerId);
    if (banner.isDefault()) {
      throw new BusinessException(ErrorCode.CONTENT_DEFAULT_BANNER);
    }
    return banner;
  }

  private Texts validate(AdminBannerRequest request, boolean isDefault) {
    Map<String, String> title = LocalizedTexts.required(request.title(), "title");
    Map<String, String> description = LocalizedTexts.optional(request.description(), "description");
    Map<String, String> ctaLabel = LocalizedTexts.optional(request.ctaLabel(), "ctaLabel");
    String targetUrl =
        request.targetUrl() == null || request.targetUrl().isBlank()
            ? null
            : request.targetUrl().strip();
    if (request.targetType() == BannerTargetType.URL && targetUrl == null) {
      throw LocalizedTexts.invalid("targetUrl", "targetType 이 URL 이면 필수입니다");
    }
    if (!isDefault
        && request.startDate() != null
        && request.endDate() != null
        && request.endDate().isBefore(request.startDate())) {
      throw LocalizedTexts.invalid("endDate", "시작일보다 빠를 수 없습니다");
    }
    return new Texts(title, description, ctaLabel, targetUrl);
  }

  private record Texts(
      Map<String, String> title,
      Map<String, String> description,
      Map<String, String> ctaLabel,
      String targetUrl) {}
}
