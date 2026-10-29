package com.seoulaim.domain.artist.repository;

import com.seoulaim.domain.artist.entity.ArtistGroup;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistGroupRepository extends JpaRepository<ArtistGroup, UUID> {

  // display_order 가 같으면 id 순 → 정렬 고정
  List<ArtistGroup> findAllByOrderByDisplayOrderAscIdAsc();
}
