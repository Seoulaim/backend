package com.seoulaim.domain.artist.controller;

import com.seoulaim.domain.artist.dto.ArtistGroupResponse;
import com.seoulaim.domain.artist.dto.ArtistResponse;
import com.seoulaim.domain.artist.dto.ArtistSearchRequest;
import com.seoulaim.domain.artist.dto.ArtistWithGroupResponse;
import com.seoulaim.domain.artist.service.ArtistService;
import com.seoulaim.global.common.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ArtistController {

  private final ArtistService artistService;

  @Operation(summary = "그룹 목록 조회")
  @GetMapping("/artist-groups")
  public List<ArtistGroupResponse> getArtistGroups() {
    return artistService.findArtistGroups();
  }

  @Operation(summary = "그룹 멤버 목록 조회")
  @GetMapping("/artist-groups/{groupId}/artists")
  public List<ArtistResponse> getArtistsByGroup(@PathVariable UUID groupId) {
    return artistService.findArtistsByGroup(groupId);
  }

  @Operation(summary = "아티스트 검색 (아티스트·그룹 이름)")
  @GetMapping("/artists")
  public CursorPageResponse<ArtistWithGroupResponse> searchArtists(
      @Parameter(description = "keyword, cursor, size(기본 20, 최대 50)") @Valid @ModelAttribute
          ArtistSearchRequest request) {
    return artistService.searchArtists(
        request.keyword(), request.cursor(), request.sizeOrDefault());
  }
}
