package com.seoulaim.domain.artist.repository;

import com.seoulaim.domain.artist.entity.Artist;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, UUID> {

  List<Artist> findAllByArtistGroupIdOrderByDisplayOrderAscIdAsc(UUID artistGroupId);
}
