package com.seoulaim.domain.place.admin.controller;

import com.seoulaim.domain.place.admin.dto.AdminKeywordListResponse;
import com.seoulaim.domain.place.admin.dto.AdminKeywordOrderRequest;
import com.seoulaim.domain.place.admin.dto.AdminKeywordRequest;
import com.seoulaim.domain.place.admin.dto.AdminKeywordResponse;
import com.seoulaim.domain.place.service.AdminKeywordService;
import com.seoulaim.global.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin")
@SecurityRequirement(name = SwaggerConfig.ADMIN_KEY_SCHEME)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/keywords")
public class AdminKeywordController {

  private final AdminKeywordService adminKeywordService;

  @Operation(summary = "[관리자] 키워드 목록 조회 (노출 순서, 장소 수 포함)")
  @GetMapping
  public AdminKeywordListResponse getKeywords() {
    return adminKeywordService.getKeywords();
  }

  @Operation(summary = "[관리자] 키워드 추가 (맨 뒤 순서, code 생략 시 영어 이름으로 생성)")
  @PostMapping
  public ResponseEntity<AdminKeywordResponse> createKeyword(
      @Valid @RequestBody AdminKeywordRequest request) {
    AdminKeywordResponse response = adminKeywordService.createKeyword(request);
    return ResponseEntity.created(URI.create("/api/v1/admin/keywords/" + response.id()))
        .body(response);
  }

  @Operation(summary = "[관리자] 키워드 순서 변경 (전체 id 를 노출 순서대로)")
  @PutMapping("/order")
  public AdminKeywordListResponse reorderKeywords(
      @Valid @RequestBody AdminKeywordOrderRequest request) {
    return adminKeywordService.reorderKeywords(request);
  }

  @Operation(summary = "[관리자] 키워드 이름 수정 (code 변경 불가)")
  @PutMapping("/{keywordId}")
  public AdminKeywordResponse updateKeyword(
      @PathVariable UUID keywordId, @Valid @RequestBody AdminKeywordRequest request) {
    return adminKeywordService.updateKeyword(keywordId, request);
  }

  @Operation(summary = "[관리자] 키워드 삭제 (장소 연결도 함께 삭제)")
  @DeleteMapping("/{keywordId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteKeyword(@PathVariable UUID keywordId) {
    adminKeywordService.deleteKeyword(keywordId);
  }
}
