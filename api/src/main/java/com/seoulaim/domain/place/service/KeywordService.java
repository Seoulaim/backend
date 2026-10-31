package com.seoulaim.domain.place.service;

import com.seoulaim.domain.place.dto.KeywordResponse;
import com.seoulaim.domain.place.repository.KeywordRepository;
import java.util.List;
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
}
