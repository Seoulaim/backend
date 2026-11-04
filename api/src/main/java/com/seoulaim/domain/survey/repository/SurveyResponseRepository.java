package com.seoulaim.domain.survey.repository;

import com.seoulaim.domain.survey.entity.SurveyResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, UUID> {

  boolean existsByUserId(UUID userId);

  // user_id unique → 최대 1건
  Optional<SurveyResponse> findByUserId(UUID userId);

  // [from, to) 구간, 최신순
  List<SurveyResponse>
      findAllBySubmittedAtGreaterThanEqualAndSubmittedAtLessThanOrderBySubmittedAtDescIdDesc(
          Instant from, Instant to);

  /** 질문별 응답 수. 결과 행: [questionId(String), count(Long)]. */
  @Query(
      value =
          """
          SELECT k.question_id, COUNT(*)
          FROM survey_responses r, jsonb_object_keys(r.answers) AS k(question_id)
          GROUP BY k.question_id
          """,
      nativeQuery = true)
  List<Object[]> countAnswersByQuestion();

  /** 이 보기를 고른 응답 수 (SINGLE 은 값 일치, MULTIPLE 은 배열 포함). */
  @Query(
      value =
          """
          SELECT COUNT(*) FROM survey_responses r
          WHERE r.answers -> CAST(:questionId AS text) = to_jsonb(CAST(:optionId AS text))
             OR r.answers -> CAST(:questionId AS text) @> jsonb_build_array(CAST(:optionId AS text))
          """,
      nativeQuery = true)
  long countOptionAnswers(
      @Param("questionId") String questionId, @Param("optionId") String optionId);
}
