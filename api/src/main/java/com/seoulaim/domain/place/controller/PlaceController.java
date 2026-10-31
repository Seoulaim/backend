package com.seoulaim.domain.place.controller;

import com.seoulaim.domain.place.dto.PlaceDetailResponse;
import com.seoulaim.domain.place.service.PlaceFavoriteService;
import com.seoulaim.domain.place.service.PlaceService;
import com.seoulaim.global.common.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceController {

  private final PlaceService placeService;
  private final PlaceFavoriteService placeFavoriteService;

  @Operation(summary = "장소 상세 조회")
  @GetMapping("/{placeId}")
  public PlaceDetailResponse getPlace(@CurrentUserId UUID userId, @PathVariable UUID placeId) {
    return placeService.getPlaceDetail(userId, placeId);
  }

  @Operation(summary = "장소 즐겨찾기 등록 (반복 호출 시 결과 동일)")
  @PutMapping("/{placeId}/favorite")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void addFavorite(@CurrentUserId UUID userId, @PathVariable UUID placeId) {
    placeFavoriteService.addFavorite(userId, placeId);
  }

  @Operation(summary = "장소 즐겨찾기 해제 (반복 호출 시 결과 동일)")
  @DeleteMapping("/{placeId}/favorite")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeFavorite(@CurrentUserId UUID userId, @PathVariable UUID placeId) {
    placeFavoriteService.removeFavorite(userId, placeId);
  }
}
