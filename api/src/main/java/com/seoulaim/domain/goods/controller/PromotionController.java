package com.seoulaim.domain.goods.controller;

import com.seoulaim.domain.goods.dto.PromotionResponse;
import com.seoulaim.domain.goods.entity.PromotionImportance;
import com.seoulaim.domain.goods.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Goods")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/promotions")
public class PromotionController {

  private final PromotionService promotionService;

  @Operation(summary = "노출 기간 내 프로모션 목록 조회")
  @GetMapping
  public List<PromotionResponse> getPromotions(@RequestParam PromotionImportance importance) {
    return promotionService.getActivePromotions(importance);
  }
}
