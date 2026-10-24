package com.seoulaim.domain.goods.service;

import com.seoulaim.domain.goods.dto.PromotionResponse;
import com.seoulaim.domain.goods.entity.PromotionImportance;
import com.seoulaim.domain.goods.repository.PromotionRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromotionService {

  private final PromotionRepository promotionRepository;

  // TODO: bias 필터링은 auth/bias API 가 develop 에 들어온 뒤 추가
  public List<PromotionResponse> getActivePromotions(PromotionImportance importance) {
    return promotionRepository.findAllActive(importance, Instant.now()).stream()
        .map(PromotionResponse::from)
        .toList();
  }
}
