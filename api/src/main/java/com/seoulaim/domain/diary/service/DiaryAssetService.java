package com.seoulaim.domain.diary.service;

import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerRequest;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerResponse;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryTemplateRequest;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryTemplateResponse;
import com.seoulaim.domain.diary.dto.DiaryEditorStickersResponse;
import com.seoulaim.domain.diary.dto.DiaryStickerCategoryResponse;
import com.seoulaim.domain.diary.dto.DiaryStickerResponse;
import com.seoulaim.domain.diary.dto.DiaryTemplateResponse;
import com.seoulaim.domain.diary.entity.DiarySticker;
import com.seoulaim.domain.diary.entity.DiaryStickerCategory;
import com.seoulaim.domain.diary.entity.DiaryTemplate;
import com.seoulaim.domain.diary.repository.DiaryStickerCategoryRepository;
import com.seoulaim.domain.diary.repository.DiaryStickerRepository;
import com.seoulaim.domain.diary.repository.DiaryTemplateRepository;
import com.seoulaim.domain.file.service.FileService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 다이어리 편집에 쓰는 템플릿·스티커. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryAssetService {

  private final DiaryTemplateRepository diaryTemplateRepository;
  private final DiaryStickerRepository diaryStickerRepository;
  private final DiaryStickerCategoryRepository diaryStickerCategoryRepository;
  private final FileService fileService;

  public List<DiaryTemplateResponse> getActiveTemplates() {
    return diaryTemplateRepository.findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc().stream()
        .map(DiaryTemplateResponse::from)
        .toList();
  }

  /** 회원 편집기용: 카테고리 전체 + 활성 스티커. 둘 다 노출 순서. */
  public DiaryEditorStickersResponse getEditorStickers() {
    List<DiaryStickerCategory> categories =
        diaryStickerCategoryRepository.findAllByOrderByDisplayOrderAscIdAsc();
    Map<UUID, Integer> order = categoryOrder(categories);
    List<DiaryStickerResponse> stickers =
        diaryStickerRepository.findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc().stream()
            .map(s -> DiaryStickerResponse.of(s, sortedCategoryIds(s, order)))
            .toList();
    return new DiaryEditorStickersResponse(
        categories.stream().map(DiaryStickerCategoryResponse::from).toList(), stickers);
  }

  /** 주어진 id 중 비활성 템플릿만. 편집 중인 다이어리가 이미 쓰고 있는 템플릿을 계속 보여주기 위해 쓴다. */
  public List<DiaryTemplateResponse> getInactiveTemplates(Collection<UUID> templateIds) {
    if (templateIds.isEmpty()) {
      return List.of();
    }
    return diaryTemplateRepository
        .findAllByIdInAndIsActiveFalseOrderByDisplayOrderAscIdAsc(templateIds)
        .stream()
        .map(DiaryTemplateResponse::from)
        .toList();
  }

  public List<AdminDiaryTemplateResponse> getAllTemplates() {
    return diaryTemplateRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
        .map(AdminDiaryTemplateResponse::from)
        .toList();
  }

  public List<AdminDiaryStickerResponse> getAllStickers() {
    Map<UUID, Integer> order = currentCategoryOrder();
    return diaryStickerRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
        .map(s -> AdminDiaryStickerResponse.of(s, sortedCategoryIds(s, order)))
        .toList();
  }

  @Transactional
  public AdminDiaryTemplateResponse createTemplate(AdminDiaryTemplateRequest request) {
    DiaryTemplate template =
        diaryTemplateRepository.save(
            DiaryTemplate.create(
                request.name(),
                request.description(),
                linkRequiredImage(request.imageFileId()),
                request.overlayFileId() == null ? null : linkImage(request.overlayFileId()),
                request.aspect() == null ? DiaryTemplate.DEFAULT_ASPECT : request.aspect(),
                request.frames() == null ? List.of() : request.frames(),
                request.displayOrder(),
                request.isActive() == null || request.isActive()));
    return AdminDiaryTemplateResponse.from(template);
  }

  /** 이미지를 교체하면 이전 파일은 LINKED 로 남는다. imageFileId·레이아웃 필드는 생략하면 기존 값을 유지한다. */
  @Transactional
  public AdminDiaryTemplateResponse updateTemplate(
      UUID templateId, AdminDiaryTemplateRequest request) {
    DiaryTemplate template =
        diaryTemplateRepository
            .findById(templateId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_TEMPLATE_NOT_FOUND));
    template.update(
        request.name(),
        request.description(),
        request.imageFileId() == null ? template.getImageKey() : linkImage(request.imageFileId()),
        request.overlayFileId() == null
            ? template.getOverlayImageKey()
            : linkImage(request.overlayFileId()),
        request.aspect() == null ? template.getAspect() : request.aspect(),
        request.frames() == null ? template.getFrames() : request.frames(),
        request.displayOrder(),
        request.isActive() == null ? template.isActive() : request.isActive());
    return AdminDiaryTemplateResponse.from(template);
  }

  /** categoryIds 를 생략하면 카테고리 없이 만든다. */
  @Transactional
  public AdminDiaryStickerResponse createSticker(AdminDiaryStickerRequest request) {
    Set<UUID> categoryIds = validateCategoryIds(request.categoryIds());
    DiarySticker sticker =
        DiarySticker.create(
            request.name(),
            linkRequiredImage(request.imageFileId()),
            request.displayOrder(),
            request.isActive() == null || request.isActive());
    if (categoryIds != null) {
      sticker.replaceCategories(categoryIds);
    }
    return toAdminResponse(diaryStickerRepository.save(sticker));
  }

  /** imageFileId·categoryIds 를 생략하면 기존 값을 유지한다. categoryIds 가 빈 배열이면 전부 해제. */
  @Transactional
  public AdminDiaryStickerResponse updateSticker(UUID stickerId, AdminDiaryStickerRequest request) {
    DiarySticker sticker =
        diaryStickerRepository
            .findById(stickerId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_STICKER_NOT_FOUND));
    Set<UUID> categoryIds = validateCategoryIds(request.categoryIds());
    sticker.update(
        request.name(),
        request.imageFileId() == null ? sticker.getImageKey() : linkImage(request.imageFileId()),
        request.displayOrder(),
        request.isActive() == null ? sticker.isActive() : request.isActive());
    if (categoryIds != null) {
      sticker.replaceCategories(categoryIds);
    }
    return toAdminResponse(sticker);
  }

  // null 이면 null(변경 없음). 중복은 합치고, 하나라도 없는 id 가 있으면 DIARY_STICKER_CATEGORY_NOT_FOUND
  private Set<UUID> validateCategoryIds(List<UUID> categoryIds) {
    if (categoryIds == null) {
      return null;
    }
    Set<UUID> requested = new LinkedHashSet<>(categoryIds);
    if (requested.isEmpty()) {
      return requested;
    }
    Set<UUID> existing =
        diaryStickerCategoryRepository.findAllById(requested).stream()
            .map(DiaryStickerCategory::getId)
            .collect(Collectors.toSet());
    List<String> missing =
        requested.stream().filter(id -> !existing.contains(id)).map(UUID::toString).toList();
    if (!missing.isEmpty()) {
      throw new BusinessException(
          ErrorCode.DIARY_STICKER_CATEGORY_NOT_FOUND,
          Map.of("categoryIds", String.join(",", missing)));
    }
    return requested;
  }

  private AdminDiaryStickerResponse toAdminResponse(DiarySticker sticker) {
    return AdminDiaryStickerResponse.of(
        sticker, sortedCategoryIds(sticker, currentCategoryOrder()));
  }

  private Map<UUID, Integer> currentCategoryOrder() {
    return categoryOrder(diaryStickerCategoryRepository.findAllByOrderByDisplayOrderAscIdAsc());
  }

  // 카테고리 id → 노출 순서상 위치
  private static Map<UUID, Integer> categoryOrder(List<DiaryStickerCategory> categories) {
    Map<UUID, Integer> order = new HashMap<>();
    for (int i = 0; i < categories.size(); i++) {
      order.put(categories.get(i).getId(), i);
    }
    return order;
  }

  // 스티커의 카테고리 id 를 카테고리 노출 순서로 정렬 (모르는 id 는 뒤로, 그 안에선 id 순)
  private static List<UUID> sortedCategoryIds(DiarySticker sticker, Map<UUID, Integer> order) {
    return sticker.getCategoryIds().stream()
        .sorted(
            Comparator.comparing((UUID id) -> order.getOrDefault(id, Integer.MAX_VALUE))
                .thenComparing(Comparator.naturalOrder()))
        .toList();
  }

  // 생성 시 imageFileId 는 필수. 수정에선 선택이라 DTO 가 아니라 여기서 검증한다
  private String linkRequiredImage(UUID fileId) {
    if (fileId == null) {
      throw new BusinessException(
          ErrorCode.COMMON_INVALID_REQUEST, Map.of("imageFileId", "생성 시 필수입니다"));
    }
    return linkImage(fileId);
  }

  // 업로드된 파일을 LINKED 로 바꾸고 s3 key 를 꺼낸다. 없는 fileId 면 FILE_NOT_FOUND
  private String linkImage(UUID fileId) {
    fileService.markLinked(List.of(fileId));
    return fileService.getById(fileId).s3Key();
  }
}
