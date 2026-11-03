package com.seoulaim.domain.artist.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 그룹 등록·수정 공용
 *
 * <ul>
 *   <li>members 배열 순서 = 멤버 displayOrder (0부터)
 *   <li>수정 시 members[].id 있음 → 기존 멤버 수정, 없음 → 새 멤버 추가
 *   <li>imageKey: 어드민 파일 업로드(domain ARTIST)로 받은 s3 key
 * </ul>
 */
public record AdminArtistGroupRequest(
    @NotBlank @Size(max = 100) String nameKo,
    @NotBlank @Size(max = 100) String nameEn,
    @Size(max = 100) String nameJa,
    @Size(max = 100) String nameZh,
    @Size(max = 512) String imageKey,
    LocalDate debutDate,
    @PositiveOrZero Integer displayOrder,
    List<@NotNull @Valid AdminArtistMemberRequest> members) {

  public record AdminArtistMemberRequest(
      UUID id,
      @NotBlank @Size(max = 100) String nameKo,
      @NotBlank @Size(max = 100) String nameEn,
      @Size(max = 100) String nameJa,
      @Size(max = 100) String nameZh,
      @Size(max = 512) String imageKey,
      LocalDate birthDate) {}

  public List<AdminArtistMemberRequest> membersOrEmpty() {
    return members == null ? List.of() : members;
  }

  public int displayOrderOrDefault() {
    return displayOrder == null ? 0 : displayOrder;
  }
}
