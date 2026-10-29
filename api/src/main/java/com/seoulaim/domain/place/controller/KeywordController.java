package com.seoulaim.domain.place.controller;

import com.seoulaim.domain.place.dto.KeywordResponse;
import com.seoulaim.domain.place.service.KeywordService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/keywords")
@RequiredArgsConstructor
public class KeywordController {

  private final KeywordService keywordService;

  @Operation(summary = "키워드 목록 조회")
  @GetMapping
  public List<KeywordResponse> getKeywords() {
    return keywordService.findKeywords();
  }
}
