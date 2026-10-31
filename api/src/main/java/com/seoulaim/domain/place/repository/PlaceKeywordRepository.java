package com.seoulaim.domain.place.repository;

import com.seoulaim.domain.place.entity.PlaceKeyword;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceKeywordRepository extends JpaRepository<PlaceKeyword, UUID> {

  @Query("SELECT pk FROM PlaceKeyword pk JOIN FETCH pk.place WHERE pk.keyword.id IN :keywordIds")
  List<PlaceKeyword> findAllWithPlaceByKeywordIdIn(
      @Param("keywordIds") Collection<UUID> keywordIds);
}
