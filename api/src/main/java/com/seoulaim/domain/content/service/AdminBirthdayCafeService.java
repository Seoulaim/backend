package com.seoulaim.domain.content.service;

import com.seoulaim.domain.artist.service.ArtistService;
import com.seoulaim.domain.content.admin.dto.AdminBirthdayCafeRequest;
import com.seoulaim.domain.content.admin.dto.AdminBirthdayCafeResponse;
import com.seoulaim.domain.content.entity.BirthdayCafe;
import com.seoulaim.domain.content.repository.BirthdayCafeRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 어드민 이벤트 관리: 생일카페 CRUD. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBirthdayCafeService {

  private final BirthdayCafeRepository birthdayCafeRepository;
  private final ArtistService artistService;
  private final ContentImageResolver contentImageResolver;

  public List<AdminBirthdayCafeResponse> getCafes() {
    return birthdayCafeRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
        .map(AdminBirthdayCafeResponse::from)
        .toList();
  }

  public AdminBirthdayCafeResponse getCafe(UUID cafeId) {
    return AdminBirthdayCafeResponse.from(findCafe(cafeId));
  }

  /** 맨 뒤 순서로 추가한다. */
  @Transactional
  public AdminBirthdayCafeResponse createCafe(AdminBirthdayCafeRequest request) {
    Map<String, String> venue = validate(request);
    BirthdayCafe cafe =
        birthdayCafeRepository.save(
            BirthdayCafe.create(
                request.artistId(),
                venue,
                request.startDate(),
                request.endDate(),
                contentImageResolver.resolve(null, request.imageFileId(), request.removeImage()),
                blankToNull(request.linkUrl()),
                birthdayCafeRepository.findMaxDisplayOrder() + 1));
    return AdminBirthdayCafeResponse.from(cafe);
  }

  @Transactional
  public AdminBirthdayCafeResponse updateCafe(UUID cafeId, AdminBirthdayCafeRequest request) {
    BirthdayCafe cafe = findCafe(cafeId);
    Map<String, String> venue = validate(request);
    cafe.update(
        request.artistId(),
        venue,
        request.startDate(),
        request.endDate(),
        contentImageResolver.resolve(
            cafe.getImageKey(), request.imageFileId(), request.removeImage()),
        blankToNull(request.linkUrl()));
    return AdminBirthdayCafeResponse.from(cafe);
  }

  @Transactional
  public void deleteCafe(UUID cafeId) {
    birthdayCafeRepository.delete(findCafe(cafeId));
  }

  private BirthdayCafe findCafe(UUID cafeId) {
    return birthdayCafeRepository
        .findById(cafeId)
        .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
  }

  private Map<String, String> validate(AdminBirthdayCafeRequest request) {
    Map<String, String> venue = LocalizedTexts.required(request.venue(), "venue");
    if (request.endDate().isBefore(request.startDate())) {
      throw LocalizedTexts.invalid("endDate", "시작일보다 빠를 수 없습니다");
    }
    if (!artistService
        .findGroupIdsByArtistIds(List.of(request.artistId()))
        .containsKey(request.artistId())) {
      throw new BusinessException(
          ErrorCode.CONTENT_INVALID_GROUP, Map.of("artistId", request.artistId().toString()));
    }
    return venue;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
