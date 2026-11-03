package com.seoulaim.domain.artist.admin.dto;

import com.seoulaim.domain.artist.entity.Artist;
import com.seoulaim.domain.artist.entity.ArtistGroup;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// 어드민 그룹 (멤버 포함). members 는 displayOrder 순
public record AdminArtistGroupResponse(
    UUID id,
    String nameKo,
    String nameEn,
    String nameJa,
    String nameZh,
    String imageKey,
    LocalDate debutDate,
    int displayOrder,
    Instant updatedAt,
    List<AdminArtistMemberResponse> members) {

  public record AdminArtistMemberResponse(
      UUID id,
      String nameKo,
      String nameEn,
      String nameJa,
      String nameZh,
      String imageKey,
      LocalDate birthDate,
      int displayOrder) {

    static AdminArtistMemberResponse from(Artist artist) {
      return new AdminArtistMemberResponse(
          artist.getId(),
          artist.getNameKo(),
          artist.getNameEn(),
          artist.getNameJa(),
          artist.getNameZh(),
          artist.getImageKey(),
          artist.getBirthDate(),
          artist.getDisplayOrder());
    }
  }

  public static AdminArtistGroupResponse of(ArtistGroup group, List<Artist> members) {
    return new AdminArtistGroupResponse(
        group.getId(),
        group.getNameKo(),
        group.getNameEn(),
        group.getNameJa(),
        group.getNameZh(),
        group.getImageKey(),
        group.getDebutDate(),
        group.getDisplayOrder(),
        group.getUpdatedAt(),
        members.stream().map(AdminArtistMemberResponse::from).toList());
  }
}
