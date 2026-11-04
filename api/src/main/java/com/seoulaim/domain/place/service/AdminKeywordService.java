package com.seoulaim.domain.place.service;

import com.seoulaim.domain.place.admin.dto.AdminKeywordListResponse;
import com.seoulaim.domain.place.admin.dto.AdminKeywordOrderRequest;
import com.seoulaim.domain.place.admin.dto.AdminKeywordRequest;
import com.seoulaim.domain.place.admin.dto.AdminKeywordResponse;
import com.seoulaim.domain.place.entity.Keyword;
import com.seoulaim.domain.place.repository.KeywordRepository;
import com.seoulaim.domain.place.repository.PlaceKeywordRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 어드민 키워드 관리 (키워드 탭)
 *
 * <ul>
 *   <li>새 키워드는 맨 뒤 순서로 추가
 *   <li>code 는 등록 후 변경 불가 (장소 등록 요청·클라이언트에서 식별자로 사용)
 *   <li>삭제 시 장소 연결도 함께 삭제. 저장된 일정에는 키워드가 남지 않아 영향 없음
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminKeywordService {

  private static final int CODE_MAX_LENGTH = 50;

  private final KeywordRepository keywordRepository;
  private final PlaceKeywordRepository placeKeywordRepository;

  public AdminKeywordListResponse getKeywords() {
    Map<UUID, Long> placeCounts =
        placeKeywordRepository.countPlacesByKeyword().stream()
            .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
    return new AdminKeywordListResponse(
        keywordRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
            .map(
                keyword ->
                    AdminKeywordResponse.of(keyword, placeCounts.getOrDefault(keyword.getId(), 0L)))
            .toList());
  }

  @Transactional
  public AdminKeywordResponse createKeyword(AdminKeywordRequest request) {
    String code =
        request.code() == null || request.code().isBlank()
            ? toCode(request.name().en())
            : request.code();
    if (keywordRepository.existsByCode(code)) {
      throw new BusinessException(ErrorCode.PLACE_KEYWORD_DUPLICATED);
    }
    Keyword keyword =
        keywordRepository.save(
            Keyword.create(
                code, request.name().toLocalized(), keywordRepository.findMaxDisplayOrder() + 1));
    return AdminKeywordResponse.of(keyword, 0L);
  }

  @Transactional
  public AdminKeywordResponse updateKeyword(UUID keywordId, AdminKeywordRequest request) {
    Keyword keyword = findKeyword(keywordId);
    keyword.rename(request.name().toLocalized());
    // updatedAt 반영
    keywordRepository.flush();
    return AdminKeywordResponse.of(keyword, placeKeywordRepository.countByKeywordId(keywordId));
  }

  // 요청 순서대로 0부터 다시 부여
  @Transactional
  public AdminKeywordListResponse reorderKeywords(AdminKeywordOrderRequest request) {
    Map<UUID, Keyword> keywords =
        keywordRepository.findAll().stream()
            .collect(Collectors.toMap(Keyword::getId, Function.identity()));
    List<UUID> orderedIds = request.keywordIds();
    Set<UUID> uniqueIds = new HashSet<>(orderedIds);
    if (orderedIds.size() != uniqueIds.size() || !uniqueIds.equals(keywords.keySet())) {
      throw new BusinessException(ErrorCode.PLACE_KEYWORD_INVALID_ORDER);
    }
    for (int order = 0; order < orderedIds.size(); order++) {
      keywords.get(orderedIds.get(order)).changeDisplayOrder(order);
    }
    keywordRepository.flush();
    return getKeywords();
  }

  @Transactional
  public void deleteKeyword(UUID keywordId) {
    Keyword keyword = findKeyword(keywordId);
    placeKeywordRepository.deleteAllByKeywordId(keywordId);
    keywordRepository.delete(keyword);
  }

  private Keyword findKeyword(UUID keywordId) {
    return keywordRepository
        .findById(keywordId)
        .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_KEYWORD_NOT_FOUND));
  }

  // 영어 이름 → 대문자·밑줄 코드 (예: "Pop-up Store" → POP_UP_STORE)
  static String toCode(String nameEn) {
    String code =
        nameEn
            .trim()
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
    if (code.isEmpty()) {
      throw new BusinessException(ErrorCode.PLACE_KEYWORD_INVALID_CODE);
    }
    return code.length() > CODE_MAX_LENGTH ? code.substring(0, CODE_MAX_LENGTH) : code;
  }
}
