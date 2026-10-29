package com.seoulaim.domain.artist.controller;

import com.seoulaim.domain.artist.dto.ArtistWithGroupResponse;
import com.seoulaim.domain.artist.dto.UpdateUserBiasesRequest;
import com.seoulaim.domain.artist.service.UserBiasService;
import com.seoulaim.global.common.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/biases")
@RequiredArgsConstructor
public class UserBiasController {

  private final UserBiasService userBiasService;

  @Operation(summary = "내 bias 목록 조회 (등록순)")
  @GetMapping
  public List<ArtistWithGroupResponse> getMyBiases(@CurrentUserId UUID userId) {
    return userBiasService.findMyBiases(userId);
  }

  @Operation(summary = "내 bias 목록 저장 (전체 교체)")
  @PutMapping
  public List<ArtistWithGroupResponse> replaceMyBiases(
      @CurrentUserId UUID userId, @Valid @RequestBody UpdateUserBiasesRequest request) {
    return userBiasService.replaceMyBiases(userId, request.artistIds());
  }
}
