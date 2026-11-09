package com.seoulaim.domain.diary.service;

import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerCategoryListResponse;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerCategoryResponse;
import com.seoulaim.domain.diary.entity.DiaryStickerCategory;
import com.seoulaim.domain.diary.repository.CategoryStickerCount;
import com.seoulaim.domain.diary.repository.DiaryStickerCategoryRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 어드민 스티커 카테고리 CRUD·정렬. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryStickerCategoryService {

  private final DiaryStickerCategoryRepository categoryRepository;

  public List<AdminDiaryStickerCategoryListResponse> getCategories() {
    Map<UUID, Long> counts =
        categoryRepository.countActiveStickersByCategory().stream()
            .collect(
                Collectors.toMap(
                    CategoryStickerCount::getCategoryId, CategoryStickerCount::getCount));
    return categoryRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
        .map(c -> AdminDiaryStickerCategoryListResponse.of(c, counts.getOrDefault(c.getId(), 0L)))
        .toList();
  }

  /** 맨 뒤(현재 최대 display_order + 1)에 붙인다. */
  @Transactional
  public AdminDiaryStickerCategoryResponse createCategory(String name) {
    String normalized = normalizeName(name);
    int nextOrder =
        categoryRepository
            .findTopByOrderByDisplayOrderDesc()
            .map(c -> c.getDisplayOrder() + 1)
            .orElse(0);
    DiaryStickerCategory category =
        categoryRepository.save(DiaryStickerCategory.create(normalized, nextOrder));
    return AdminDiaryStickerCategoryResponse.from(category);
  }

  @Transactional
  public AdminDiaryStickerCategoryResponse updateCategory(UUID categoryId, String name) {
    String normalized = normalizeName(name);
    DiaryStickerCategory category = getCategory(categoryId);
    category.rename(normalized);
    return AdminDiaryStickerCategoryResponse.from(category);
  }

  /** 스티커와의 매핑만 지운다. 스티커는 그대로 남는다. */
  @Transactional
  public void deleteCategory(UUID categoryId) {
    DiaryStickerCategory category = getCategory(categoryId);
    categoryRepository.deleteMappingsByCategoryId(categoryId);
    categoryRepository.deleteById(category.getId());
  }

  /**
   * ids 순서대로 0, 1, 2… 를 주고, 빠진 카테고리는 기존 순서를 유지한 채 그 뒤에 둔다. 중복 id 는 400
   * DIARY_STICKER_CATEGORY_INVALID, 없는 id 는 404 DIARY_STICKER_CATEGORY_NOT_FOUND.
   */
  @Transactional
  public void reorderCategories(List<UUID> ids) {
    Set<UUID> seen = new HashSet<>();
    for (UUID id : ids) {
      if (!seen.add(id)) {
        throw new BusinessException(
            ErrorCode.DIARY_STICKER_CATEGORY_INVALID, Map.of("ids", "중복된 id 가 있습니다: " + id));
      }
    }
    Map<UUID, DiaryStickerCategory> byId =
        categoryRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
            .collect(
                Collectors.toMap(
                    DiaryStickerCategory::getId,
                    Function.identity(),
                    (a, b) -> a,
                    LinkedHashMap::new));
    List<DiaryStickerCategory> ordered = new ArrayList<>(byId.size());
    for (UUID id : ids) {
      DiaryStickerCategory category = byId.remove(id);
      if (category == null) {
        throw new BusinessException(
            ErrorCode.DIARY_STICKER_CATEGORY_NOT_FOUND, Map.of("ids", id.toString()));
      }
      ordered.add(category);
    }
    ordered.addAll(byId.values());
    for (int i = 0; i < ordered.size(); i++) {
      ordered.get(i).changeDisplayOrder(i);
    }
  }

  private DiaryStickerCategory getCategory(UUID categoryId) {
    return categoryRepository
        .findById(categoryId)
        .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_STICKER_CATEGORY_NOT_FOUND));
  }

  // 앞뒤 공백 제거 후 1~50자
  private static String normalizeName(String name) {
    String normalized = name == null ? "" : name.strip();
    if (normalized.isEmpty() || normalized.length() > DiaryStickerCategory.NAME_MAX_LENGTH) {
      throw new BusinessException(
          ErrorCode.DIARY_STICKER_CATEGORY_INVALID,
          Map.of("name", "1~" + DiaryStickerCategory.NAME_MAX_LENGTH + "자여야 합니다"));
    }
    return normalized;
  }
}
