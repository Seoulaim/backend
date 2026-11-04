package com.seoulaim.domain.place.repository;

import com.seoulaim.domain.place.entity.Keyword;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface KeywordRepository extends JpaRepository<Keyword, UUID> {

  List<Keyword> findAllByOrderByDisplayOrderAscIdAsc();

  long countByIdIn(Collection<UUID> ids);

  List<Keyword> findAllByCodeIn(Collection<String> codes);

  boolean existsByCode(String code);

  // 키워드가 없으면 -1
  @Query("SELECT COALESCE(MAX(k.displayOrder), -1) FROM Keyword k")
  int findMaxDisplayOrder();
}
