package com.seoulaim.domain.place.service;

import com.seoulaim.domain.place.dto.KeywordResponse;
import com.seoulaim.domain.place.repository.KeywordRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class KeywordService {

  private final KeywordRepository keywordRepository;

  public List<KeywordResponse> findKeywords() {
    return keywordRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
        .map(KeywordResponse::from)
        .toList();
  }

  // 없는 키워드가 섞이면 PLACE_KEYWORD_NOT_FOUND
  public void validateKeywordsExist(Collection<UUID> keywordIds) {
    if (keywordIds.isEmpty()) {
      return;
    }
    if (keywordRepository.countByIdIn(keywordIds) != new HashSet<>(keywordIds).size()) {
      throw new BusinessException(ErrorCode.PLACE_KEYWORD_NOT_FOUND);
    }
  }
}
