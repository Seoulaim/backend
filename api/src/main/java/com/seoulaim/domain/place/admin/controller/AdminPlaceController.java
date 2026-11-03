package com.seoulaim.domain.place.admin.controller;

import com.seoulaim.domain.place.admin.dto.AdminPlaceDetailResponse;
import com.seoulaim.domain.place.admin.dto.AdminPlaceListRequest;
import com.seoulaim.domain.place.admin.dto.AdminPlaceListResponse;
import com.seoulaim.domain.place.admin.dto.AdminPlaceRequest;
import com.seoulaim.domain.place.service.AdminPlaceService;
import com.seoulaim.global.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
@RequestMapping("/api/v1/admin/places")
public class AdminPlaceController {

  private final AdminPlaceService adminPlaceService;

  @Operation(summary = "[관리자] 장소 목록 조회 (검색·필터, 최신 등록순 커서 페이징)")
  @GetMapping
  public AdminPlaceListResponse getPlaces(
      @ParameterObject @Valid @ModelAttribute AdminPlaceListRequest request) {
    return adminPlaceService.getPlaces(request);
  }

  @Operation(summary = "[관리자] 장소 상세 조회 (수정 화면용, 숨김 장소 포함)")
  @GetMapping("/{placeId}")
  public AdminPlaceDetailResponse getPlace(@PathVariable UUID placeId) {
    return adminPlaceService.getPlace(placeId);
  }

  @Operation(summary = "[관리자] 장소 등록")
  @PostMapping
  public ResponseEntity<AdminPlaceDetailResponse> createPlace(
      @Valid @RequestBody AdminPlaceRequest request) {
    AdminPlaceDetailResponse response = adminPlaceService.createPlace(request);
    return ResponseEntity.created(URI.create("/api/v1/admin/places/" + response.id()))
        .body(response);
  }

  @Operation(summary = "[관리자] 장소 수정 (사진·운영 기간·영업시간·키워드·멤버 태그 전체 교체)")
  @PutMapping("/{placeId}")
  public AdminPlaceDetailResponse updatePlace(
      @PathVariable UUID placeId, @Valid @RequestBody AdminPlaceRequest request) {
    return adminPlaceService.updatePlace(placeId, request);
  }

  @Operation(summary = "[관리자] 장소 삭제 (일정에 포함된 장소는 409, 숨김 처리 권장)")
  @DeleteMapping("/{placeId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletePlace(@PathVariable UUID placeId) {
    adminPlaceService.deletePlace(placeId);
  }
}
