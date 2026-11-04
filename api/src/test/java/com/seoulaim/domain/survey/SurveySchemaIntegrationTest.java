package com.seoulaim.domain.survey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.seoulaim.domain.survey.entity.SurveyGiftStatus;
import com.seoulaim.domain.survey.entity.SurveyOption;
import com.seoulaim.domain.survey.entity.SurveyPopupState;
import com.seoulaim.domain.survey.entity.SurveyQuestion;
import com.seoulaim.domain.survey.entity.SurveyQuestionType;
import com.seoulaim.domain.survey.entity.SurveyResponse;
import com.seoulaim.domain.survey.entity.SurveySettings;
import com.seoulaim.domain.survey.repository.SurveyOptionRepository;
import com.seoulaim.domain.survey.repository.SurveyPopupStateRepository;
import com.seoulaim.domain.survey.repository.SurveyQuestionRepository;
import com.seoulaim.domain.survey.repository.SurveyResponseRepository;
import com.seoulaim.domain.survey.repository.SurveySettingsRepository;
import com.seoulaim.domain.survey.service.SurveySettingsInitializer;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@SpringBootTest
@Testcontainers
class SurveySchemaIntegrationTest {

  private static final Instant NOW = Instant.parse("2026-11-04T03:00:00Z");

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @Autowired private UserRepository userRepository;
  @Autowired private SurveySettingsRepository surveySettingsRepository;
  @Autowired private SurveyQuestionRepository surveyQuestionRepository;
  @Autowired private SurveyOptionRepository surveyOptionRepository;
  @Autowired private SurveyPopupStateRepository surveyPopupStateRepository;
  @Autowired private SurveyResponseRepository surveyResponseRepository;
  @Autowired private SurveySettingsInitializer surveySettingsInitializer;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private TransactionTemplate transactionTemplate;

  @Test
  @DisplayName("기동 시 설정 행이 1개 생성되고, 다시 실행해도 늘지 않으며 두 번째 행 insert 는 실패한다")
  void ensureSettings_whenStarted_keepsSingleRow() {
    assertThat(surveySettingsRepository.count()).isEqualTo(1);
    SurveySettings settings =
        surveySettingsRepository.findFirstByOrderByCreatedAtAsc().orElseThrow();
    assertThat(settings.isEnabled()).isFalse();
    assertThat(settings.getInviteTitle()).isEmpty();

    surveySettingsInitializer.ensureSettings();
    assertThat(surveySettingsRepository.count()).isEqualTo(1);

    assertThatThrownBy(() -> surveySettingsRepository.saveAndFlush(SurveySettings.createDefault()))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(surveySettingsRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("질문·보기의 다국어 문구가 jsonb 로 저장·조회되고, 숨긴 질문은 회원용 조회에서 빠진다")
  void save_whenQuestionsSaved_localizedTextRoundTrips() {
    SurveyQuestion rating =
        surveyQuestionRepository.save(
            SurveyQuestion.create(
                SurveyQuestionType.RATING,
                Map.of("en", "How happy?", "ko", "만족하세요?"),
                true,
                false,
                0));
    SurveyQuestion archived =
        surveyQuestionRepository.save(
            SurveyQuestion.create(SurveyQuestionType.TEXT, Map.of("en", "Old"), false, true, 1));
    SurveyQuestion single =
        surveyQuestionRepository.save(
            SurveyQuestion.create(
                SurveyQuestionType.SINGLE, Map.of("en", "Best?"), false, false, 2));
    surveyOptionRepository.save(SurveyOption.create(single.getId(), Map.of("en", "B"), 1));
    surveyOptionRepository.save(
        SurveyOption.create(single.getId(), Map.of("en", "A", "zh", "甲"), 0));
    surveyQuestionRepository.flush();
    surveyOptionRepository.flush();

    assertThat(surveyQuestionRepository.findAllByArchivedFalseOrderByDisplayOrderAscIdAsc())
        .extracting(SurveyQuestion::getId)
        .containsSubsequence(rating.getId(), single.getId())
        .doesNotContain(archived.getId());
    assertThat(surveyQuestionRepository.findById(rating.getId()).orElseThrow().getTitle())
        .containsEntry("ko", "만족하세요?");
    assertThat(
            surveyOptionRepository.findAllByQuestionIdInOrderByDisplayOrderAscIdAsc(
                List.of(single.getId())))
        .extracting(option -> option.getLabel().get("en"))
        .containsExactly("A", "B");
  }

  @Test
  @DisplayName("팝업 카운트는 upsert 로 누적되고, 합계 조회에 반영된다")
  void increment_whenCalledRepeatedly_accumulates() {
    UUID userId = saveUser();
    Instant until = Instant.parse("2026-11-04T15:00:00Z");
    transactionTemplate.executeWithoutResult(
        status -> {
          surveyPopupStateRepository.incrementTravelVisits(userId, NOW);
          surveyPopupStateRepository.incrementTravelVisits(userId, NOW);
          surveyPopupStateRepository.incrementShown(userId, NOW);
          surveyPopupStateRepository.incrementClosed(userId, NOW);
          surveyPopupStateRepository.incrementAccepted(userId, NOW);
          surveyPopupStateRepository.snooze(userId, until, NOW);
          surveyPopupStateRepository.snooze(userId, until, NOW);
        });

    SurveyPopupState state = surveyPopupStateRepository.findById(userId).orElseThrow();
    assertThat(state.getTravelVisits()).isEqualTo(2);
    assertThat(state.getShownCount()).isEqualTo(1);
    assertThat(state.getClosedCount()).isEqualTo(1);
    assertThat(state.getAcceptedCount()).isEqualTo(1);
    assertThat(state.getSnoozedCount()).isEqualTo(2);
    assertThat(state.getSnoozedUntil()).isEqualTo(until);

    SurveyPopupStateRepository.PopupTotals totals = surveyPopupStateRepository.sumCounts();
    assertThat(totals.getShown()).isGreaterThanOrEqualTo(1);
    assertThat(totals.getSnoozed()).isGreaterThanOrEqualTo(2);
  }

  @Test
  @DisplayName("응답은 사용자당 하나이고, 질문별 응답 수와 기간 조회가 동작한다")
  void save_whenResponseSaved_uniquePerUserAndCountsByQuestion() {
    UUID userId = saveUser();
    String questionId = UUID.randomUUID().toString();
    Map<String, Object> answers = Map.of(questionId, 5, "other-" + questionId, List.of("o1", "o2"));
    Map<String, Object> plan =
        Map.of("planId", UUID.randomUUID().toString(), "startDate", "2026-11-10", "dayCount", 2);
    SurveyResponse response =
        surveyResponseRepository.saveAndFlush(
            SurveyResponse.create(userId, "a@test.com", plan, answers, NOW));

    SurveyResponse found = surveyResponseRepository.findById(response.getId()).orElseThrow();
    assertThat(found.getAnswers()).isEqualTo(answers);
    assertThat(found.getPlanSnapshot()).isEqualTo(plan);
    assertThat(found.getGiftStatus()).isEqualTo(SurveyGiftStatus.PENDING);
    assertThat(surveyResponseRepository.existsByUserId(userId)).isTrue();

    Map<String, Long> counts =
        surveyResponseRepository.countAnswersByQuestion().stream()
            .collect(
                Collectors.toMap(row -> (String) row[0], row -> ((Number) row[1]).longValue()));
    assertThat(counts).containsEntry(questionId, 1L);

    assertThat(
            surveyResponseRepository
                .findAllBySubmittedAtGreaterThanEqualAndSubmittedAtLessThanOrderBySubmittedAtDescIdDesc(
                    NOW, NOW.plusSeconds(1)))
        .extracting(SurveyResponse::getId)
        .contains(response.getId());

    assertThatThrownBy(
            () ->
                surveyResponseRepository.saveAndFlush(
                    SurveyResponse.create(userId, "a@test.com", plan, answers, NOW)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("설정 touch 는 updated_at 만 갱신한다")
  void touch_whenCalled_updatesUpdatedAt() {
    Instant later = Instant.parse("2030-01-01T00:00:00Z");
    transactionTemplate.executeWithoutResult(status -> surveySettingsRepository.touch(later));

    Instant updatedAt =
        jdbcTemplate
            .queryForObject("SELECT updated_at FROM survey_settings", java.sql.Timestamp.class)
            .toInstant();
    assertThat(updatedAt).isEqualTo(later);
  }

  private UUID saveUser() {
    return userRepository
        .save(User.createWithEmail(UUID.randomUUID() + "@test.com", "hash", "tester"))
        .getId();
  }
}
