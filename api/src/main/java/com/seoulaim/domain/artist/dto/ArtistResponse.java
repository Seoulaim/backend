package com.seoulaim.domain.artist.dto;

import com.seoulaim.domain.artist.entity.Artist;
import java.util.UUID;

public record ArtistResponse(
    UUID id, String nameEn, String nameKo, String nameJa, String nameZh, String imageKey) {

  public static ArtistResponse from(Artist artist) {
    return new ArtistResponse(
        artist.getId(),
        artist.getNameEn(),
        artist.getNameKo(),
        artist.getNameJa(),
        artist.getNameZh(),
        artist.getImageKey());
  }
}
