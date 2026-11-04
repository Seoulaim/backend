package com.seoulaim.domain.survey.service;

import com.seoulaim.domain.plan.dto.PlanSnapshot;
import com.seoulaim.domain.plan.service.PlanService;
import com.seoulaim.domain.survey.dto.PlanSurveyResponse;
import com.seoulaim.domain.survey.dto.SurveyEventType;
import com.seoulaim.domain.survey.dto.SurveyInvite;
import com.seoulaim.domain.survey.dto.SurveyQuestionResponse;
import com.seoulaim.domain.survey.dto.TravelVisitResponse;
import com.seoulaim.domain.survey.entity.SurveyOption;
import com.seoulaim.domain.survey.entity.SurveyPopupState;
import com.seoulaim.domain.survey.entity.SurveyQuestion;
import com.seoulaim.domain.survey.entity.SurveyResponse;
import com.seoulaim.domain.survey.entity.SurveySettings;
import com.seoulaim.domain.survey.repository.SurveyOptionRepository;
import com.seoulaim.domain.survey.repository.SurveyPopupStateRepository;
import com.seoulaim.domain.survey.repository.SurveyQuestionRepository;
import com.seoulaim.domain.survey.repository.SurveyResponseRepository;
import com.seoulaim.domain.survey.repository.SurveySettingsRepository;
import com.seoulaim.domain.user.service.UserService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 앱의 플랜 만족도 조사: 초대 팝업 노출 판단, 팝업 이벤트 기록, 설문 조회·응답.
 *
 * <p>초대 노출 조건 (전부 만족): 설정 enabled, 플랜 1개 이상, TRAVEL 방문 2회 이상, 미응답, 스누즈 중 아님
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SurveyService {

  static final ZoneId KST = ZoneId.of("Asia/Seoul");
  static final int MIN_TRAVEL_VISITS = 2;
  static final int MIN_RATING = 1;
  static final int MAX_RATING = 5;
  static final int MAX_TEXT_LENGTH = 1000;

  private final SurveySettingsRepository surveySettingsRepository;
  private final SurveyQuestionRepository surveyQuestionRepository;
  private final SurveyOptionRepository surveyOptionRepository;
  private final SurveyPopupStateRepository surveyPopupStateRepository;
  private final SurveyResponseRepository surveyResponseRepository;
  private final PlanService planService;
  private final UserService userService;
  private final Clock clock;

  /** TRAVEL 방문을 세고(플랜이 있을 때만) 지금 띄울 초대 문구를 돌려준다. */
  @Transactional
  public TravelVisitResponse recordTravelVisit(UUID userId) {
    if (!planService.hasPlan(userId)) {
      return new TravelVisitResponse(null);
    }
    Instant now = clock.instant();
    surveyPopupStateRepository.incrementTravelVisits(userId, now);

    SurveySettings settings = getSettings();
    if (!settings.isEnabled()) {
      return new TravelVisitResponse(null);
    }
    SurveyPopupState state = surveyPopupStateRepository.findById(userId).orElseThrow();
    if (state.getTravelVisits() < MIN_TRAVEL_VISITS || state.isSnoozedAt(now)) {
      return new TravelVisitResponse(null);
    }
    if (surveyResponseRepository.existsByUserId(userId)) {
      return new TravelVisitResponse(null);
    }
    return new TravelVisitResponse(inviteOf(settings));
  }

  // 회원 탈퇴용: 응답은 남기고 이메일만 익명화 (응답 통계 유지)
  @Transactional
  public void anonymizeResponseOfUser(UUID userId) {
    surveyResponseRepository.findByUserId(userId).ifPresent(SurveyResponse::anonymizeEmail);
  }

  @Transactional
  public void recordEvent(UUID userId, SurveyEventType type) {
    Instant now = clock.instant();
    switch (type) {
      case SHOWN -> surveyPopupStateRepository.incrementShown(userId, now);
      case CLOSED -> surveyPopupStateRepository.incrementClosed(userId, now);
      case ACCEPTED -> surveyPopupStateRepository.incrementAccepted(userId, now);
      case SNOOZED -> surveyPopupStateRepository.snooze(userId, nextKstMidnight(now), now);
    }
  }

  public PlanSurveyResponse getPlanSurvey(UUID userId) {
    if (!planService.hasPlan(userId)) {
      throw new BusinessException(ErrorCode.PLAN_NOT_FOUND);
    }
    List<SurveyQuestion> questions =
        surveyQuestionRepository.findAllByArchivedFalseOrderByDisplayOrderAscIdAsc();
    Map<UUID, List<SurveyOption>> options = optionsByQuestionId(questions);
    return new PlanSurveyResponse(
        inviteOf(getSettings()),
        questions.stream()
            .map(q -> SurveyQuestionResponse.of(q, options.getOrDefault(q.getId(), List.of())))
            .toList(),
        userService.getUserProfile(userId).email(),
        surveyResponseRepository.existsByUserId(userId));
  }

  @Transactional
  public void submit(UUID userId, Map<String, Object> answers) {
    if (surveyResponseRepository.existsByUserId(userId)) {
      throw new BusinessException(ErrorCode.SURVEY_ALREADY_ANSWERED);
    }
    PlanSnapshot plan =
        planService
            .findLatestPlanSnapshot(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));
    List<SurveyQuestion> questions =
        surveyQuestionRepository.findAllByArchivedFalseOrderByDisplayOrderAscIdAsc();
    Map<String, Object> normalized =
        validateAnswers(questions, optionsByQuestionId(questions), answers);

    String email = userService.getUserProfile(userId).email();
    if (email == null || email.isBlank()) {
      throw new BusinessException(ErrorCode.SURVEY_INVALID, Map.of("email", "계정 이메일이 없습니다"));
    }
    try {
      surveyResponseRepository.saveAndFlush(
          SurveyResponse.create(userId, email, snapshotOf(plan), normalized, clock.instant()));
    } catch (DataIntegrityViolationException exception) {
      // 동시 제출 → user_id unique 위반
      throw new BusinessException(ErrorCode.SURVEY_ALREADY_ANSWERED);
    }
  }

  /**
   * 응답 검증 후 저장할 형태로 정리한다. null·빈 글·빈 배열은 답하지 않은 것으로 보고 뺀다.
   *
   * <ul>
   *   <li>RATING: 1~5 정수
   *   <li>SINGLE: 그 질문의 보기 id
   *   <li>MULTIPLE: 그 질문의 보기 id 배열 (중복 불가)
   *   <li>TEXT: 문자열 1000자 이하 (앞뒤 공백 제거)
   * </ul>
   */
  Map<String, Object> validateAnswers(
      List<SurveyQuestion> questions,
      Map<UUID, List<SurveyOption>> optionsByQuestionId,
      Map<String, Object> answers) {
    Map<String, SurveyQuestion> questionsById =
        questions.stream()
            .collect(Collectors.toMap(q -> q.getId().toString(), q -> q, (a, b) -> a));
    Map<String, Object> normalized = new LinkedHashMap<>();

    for (Map.Entry<String, Object> entry : answers.entrySet()) {
      SurveyQuestion question = questionsById.get(entry.getKey());
      if (question == null) {
        throw invalid(entry.getKey(), "없거나 숨겨진 질문입니다");
      }
      Set<String> optionIds =
          optionsByQuestionId.getOrDefault(question.getId(), List.of()).stream()
              .map(option -> option.getId().toString())
              .collect(Collectors.toSet());
      Object value = normalizeAnswer(question, optionIds, entry.getValue());
      if (value != null) {
        normalized.put(entry.getKey(), value);
      }
    }

    for (SurveyQuestion question : questions) {
      if (question.isRequired() && !normalized.containsKey(question.getId().toString())) {
        throw invalid(question.getId().toString(), "필수 질문입니다");
      }
    }
    return normalized;
  }

  private Object normalizeAnswer(SurveyQuestion question, Set<String> optionIds, Object value) {
    if (value == null) {
      return null;
    }
    String key = question.getId().toString();
    return switch (question.getType()) {
      case RATING -> {
        if (!(value instanceof Integer || value instanceof Long || value instanceof Short)) {
          throw invalid(key, "별점은 정수여야 합니다");
        }
        long rating = ((Number) value).longValue();
        if (rating < MIN_RATING || rating > MAX_RATING) {
          throw invalid(key, "별점은 1~5 입니다");
        }
        yield (int) rating;
      }
      case SINGLE -> {
        if (!(value instanceof String optionId)) {
          throw invalid(key, "보기 id 여야 합니다");
        }
        if (optionId.isBlank()) {
          yield null;
        }
        if (!optionIds.contains(optionId)) {
          throw invalid(key, "이 질문의 보기가 아닙니다");
        }
        yield optionId;
      }
      case MULTIPLE -> {
        if (!(value instanceof List<?> list)) {
          throw invalid(key, "보기 id 배열이어야 합니다");
        }
        if (list.isEmpty()) {
          yield null;
        }
        List<String> picked = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Object item : list) {
          if (!(item instanceof String optionId) || !optionIds.contains(optionId)) {
            throw invalid(key, "이 질문의 보기가 아닙니다");
          }
          if (!seen.add(optionId)) {
            throw invalid(key, "같은 보기를 두 번 골랐습니다");
          }
          picked.add(optionId);
        }
        yield picked;
      }
      case TEXT -> {
        if (!(value instanceof String text)) {
          throw invalid(key, "글이어야 합니다");
        }
        String trimmed = text.strip();
        if (trimmed.isEmpty()) {
          yield null;
        }
        if (trimmed.length() > MAX_TEXT_LENGTH) {
          throw invalid(key, "1000자 이하로 입력해주세요");
        }
        yield trimmed;
      }
    };
  }

  private SurveySettings getSettings() {
    return surveySettingsRepository
        .findFirstByOrderByCreatedAtAsc()
        .orElseThrow(() -> new IllegalStateException("survey_settings 행이 없습니다"));
  }

  private Map<UUID, List<SurveyOption>> optionsByQuestionId(List<SurveyQuestion> questions) {
    if (questions.isEmpty()) {
      return Map.of();
    }
    return surveyOptionRepository
        .findAllByQuestionIdInOrderByDisplayOrderAscIdAsc(
            questions.stream().map(SurveyQuestion::getId).toList())
        .stream()
        .collect(Collectors.groupingBy(SurveyOption::getQuestionId));
  }

  private static SurveyInvite inviteOf(SurveySettings settings) {
    return new SurveyInvite(settings.getInviteTitle(), settings.getInviteBody());
  }

  // jsonb 로 저장하는 응답 시점 플랜 요약. 날짜는 ISO 문자열
  private static Map<String, Object> snapshotOf(PlanSnapshot plan) {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    snapshot.put("planId", plan.planId().toString());
    snapshot.put("startDate", plan.startDate().toString());
    snapshot.put("endDate", plan.endDate().toString());
    snapshot.put("dayCount", plan.dayCount());
    snapshot.put("placeCount", plan.placeCount());
    return snapshot;
  }

  // '오늘 하루 보지 않기' → 그날(한국 시간) 자정까지
  static Instant nextKstMidnight(Instant now) {
    return LocalDate.ofInstant(now, KST).plusDays(1).atStartOfDay(KST).toInstant();
  }

  private static BusinessException invalid(String field, String reason) {
    return new BusinessException(ErrorCode.SURVEY_INVALID, Map.of(field, reason));
  }
}
