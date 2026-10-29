package com.seoulaim.domain.artist.dto;

import com.seoulaim.domain.artist.entity.Artist;
import com.seoulaim.domain.artist.entity.ArtistGroup;
import java.util.UUID;

public record ArtistWithGroupResponse(
    UUID id,
    String nameEn,
    String nameKo,
    String nameJa,
    String nameZh,
    String imageKey,
    GroupSummary artistGroup) {

  public record GroupSummary(UUID id, String nameEn, String nameKo) {}

  public static ArtistWithGroupResponse of(Artist artist, ArtistGroup group) {
    return new ArtistWithGroupResponse(
        artist.getId(),
        artist.getNameEn(),
        artist.getNameKo(),
        artist.getNameJa(),
        artist.getNameZh(),
        artist.getImageKey(),
        new GroupSummary(group.getId(), group.getNameEn(), group.getNameKo()));
  }
}
