package com.seoulaim.domain.place.repository;

import com.seoulaim.domain.place.entity.Keyword;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KeywordRepository extends JpaRepository<Keyword, UUID> {

  List<Keyword> findAllByOrderByDisplayOrderAscIdAsc();
}
