package com.seoulaim.domain.survey.repository;

import com.seoulaim.domain.survey.entity.SurveySettings;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SurveySettingsRepository extends JpaRepository<SurveySettings, UUID> {

  // 행은 1개만 있다 (uk_survey_settings_singleton)
  Optional<SurveySettings> findFirstByOrderByCreatedAtAsc();

  // 질문만 바뀌어도 설문 수정 시각(updatedAt)을 갱신한다
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(value = "UPDATE survey_settings SET updated_at = :now", nativeQuery = true)
  void touch(@Param("now") Instant now);
}
