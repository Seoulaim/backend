package com.seoulaim.domain.diary.repository;

import com.seoulaim.domain.diary.entity.DiaryTemplate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryTemplateRepository extends JpaRepository<DiaryTemplate, UUID> {

  List<DiaryTemplate> findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc();

  List<DiaryTemplate> findAllByOrderByDisplayOrderAscIdAsc();
}
