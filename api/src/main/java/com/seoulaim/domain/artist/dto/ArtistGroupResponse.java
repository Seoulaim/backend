package com.seoulaim.domain.artist.dto;

import com.seoulaim.domain.artist.entity.ArtistGroup;
import java.util.UUID;

public record ArtistGroupResponse(
    UUID id, String nameEn, String nameKo, String nameJa, String nameZh, String imageKey) {

  public static ArtistGroupResponse from(ArtistGroup group) {
    return new ArtistGroupResponse(
        group.getId(),
        group.getNameEn(),
        group.getNameKo(),
        group.getNameJa(),
        group.getNameZh(),
        group.getImageKey());
  }
}
