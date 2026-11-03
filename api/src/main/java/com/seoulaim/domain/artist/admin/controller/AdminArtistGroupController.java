package com.seoulaim.domain.artist.admin.controller;

import com.seoulaim.domain.artist.admin.dto.AdminArtistGroupListResponse;
import com.seoulaim.domain.artist.admin.dto.AdminArtistGroupRequest;
import com.seoulaim.domain.artist.admin.dto.AdminArtistGroupResponse;
import com.seoulaim.domain.artist.service.AdminArtistGroupService;
import com.seoulaim.global.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin")
@SecurityRequirement(name = SwaggerConfig.ADMIN_KEY_SCHEME)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/artist-groups")
public class AdminArtistGroupController {

  private final AdminArtistGroupService adminArtistGroupService;

  @Operation(summary = "[관리자] 그룹 목록 조회 (멤버 포함)")
  @GetMapping
  public AdminArtistGroupListResponse getGroups() {
    return adminArtistGroupService.getGroups();
  }

  @Operation(summary = "[관리자] 그룹 등록 (멤버 포함)")
  @PostMapping
  public ResponseEntity<AdminArtistGroupResponse> createGroup(
      @Valid @RequestBody AdminArtistGroupRequest request) {
    AdminArtistGroupResponse response = adminArtistGroupService.createGroup(request);
    return ResponseEntity.created(URI.create("/api/v1/admin/artist-groups/" + response.id()))
        .body(response);
  }

  @Operation(summary = "[관리자] 그룹 수정 (멤버 추가·수정, 빠진 멤버는 삭제하지 않음)")
  @PutMapping("/{groupId}")
  public AdminArtistGroupResponse updateGroup(
      @PathVariable UUID groupId, @Valid @RequestBody AdminArtistGroupRequest request) {
    return adminArtistGroupService.updateGroup(groupId, request);
  }
}
