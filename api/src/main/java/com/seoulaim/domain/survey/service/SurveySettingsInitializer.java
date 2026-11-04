package com.seoulaim.domain.survey.service;

import com.seoulaim.domain.survey.entity.SurveySettings;
import com.seoulaim.domain.survey.repository.SurveySettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * 기동 시 만족도 조사 설정 행이 없으면 기본값(꺼짐, 빈 문구)으로 만든다.
 *
 * <p>여러 인스턴스가 동시에 기동해도 uk_survey_settings_singleton 때문에 한 행만 남는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SurveySettingsInitializer {

  private final SurveySettingsRepository surveySettingsRepository;

  @EventListener(ApplicationReadyEvent.class)
  public void ensureSettings() {
    if (surveySettingsRepository.count() > 0) {
      return;
    }
    try {
      surveySettingsRepository.saveAndFlush(SurveySettings.createDefault());
      log.info("survey_settings 기본 행을 생성했습니다");
    } catch (DataIntegrityViolationException exception) {
      log.info("survey_settings 행이 이미 있어 생성을 건너뜁니다");
    }
  }
}
