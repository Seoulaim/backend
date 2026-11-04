package com.seoulaim.domain.survey.repository;

import com.seoulaim.domain.survey.entity.SurveyPopupState;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 카운트는 upsert 로 원자적으로 증가시킨다 (행이 없으면 1 로 생성). */
public interface SurveyPopupStateRepository extends JpaRepository<SurveyPopupState, UUID> {

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      value =
          """
          INSERT INTO survey_popup_states (user_id, travel_visits, created_at, updated_at)
          VALUES (:userId, 1, :now, :now)
          ON CONFLICT (user_id) DO UPDATE
          SET travel_visits = survey_popup_states.travel_visits + 1, updated_at = :now
          """,
      nativeQuery = true)
  void incrementTravelVisits(@Param("userId") UUID userId, @Param("now") Instant now);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      value =
          """
          INSERT INTO survey_popup_states (user_id, shown_count, created_at, updated_at)
          VALUES (:userId, 1, :now, :now)
          ON CONFLICT (user_id) DO UPDATE
          SET shown_count = survey_popup_states.shown_count + 1, updated_at = :now
          """,
      nativeQuery = true)
  void incrementShown(@Param("userId") UUID userId, @Param("now") Instant now);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      value =
          """
          INSERT INTO survey_popup_states (user_id, closed_count, created_at, updated_at)
          VALUES (:userId, 1, :now, :now)
          ON CONFLICT (user_id) DO UPDATE
          SET closed_count = survey_popup_states.closed_count + 1, updated_at = :now
          """,
      nativeQuery = true)
  void incrementClosed(@Param("userId") UUID userId, @Param("now") Instant now);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      value =
          """
          INSERT INTO survey_popup_states (user_id, accepted_count, created_at, updated_at)
          VALUES (:userId, 1, :now, :now)
          ON CONFLICT (user_id) DO UPDATE
          SET accepted_count = survey_popup_states.accepted_count + 1, updated_at = :now
          """,
      nativeQuery = true)
  void incrementAccepted(@Param("userId") UUID userId, @Param("now") Instant now);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      value =
          """
          INSERT INTO survey_popup_states
            (user_id, snoozed_count, snoozed_until, created_at, updated_at)
          VALUES (:userId, 1, :until, :now, :now)
          ON CONFLICT (user_id) DO UPDATE
          SET snoozed_count = survey_popup_states.snoozed_count + 1,
              snoozed_until = :until,
              updated_at = :now
          """,
      nativeQuery = true)
  void snooze(
      @Param("userId") UUID userId, @Param("until") Instant until, @Param("now") Instant now);

  /** 어드민 결과 화면용 전체 누적 합계. */
  @Query(
      """
      SELECT COALESCE(SUM(s.shownCount), 0) AS shown,
             COALESCE(SUM(s.closedCount), 0) AS closed,
             COALESCE(SUM(s.acceptedCount), 0) AS accepted,
             COALESCE(SUM(s.snoozedCount), 0) AS snoozed
      FROM SurveyPopupState s
      """)
  PopupTotals sumCounts();

  interface PopupTotals {
    long getShown();

    long getClosed();

    long getAccepted();

    long getSnoozed();
  }
}
