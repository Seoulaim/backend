package com.seoulaim.domain.survey.service;

import com.seoulaim.domain.plan.service.PlanService;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyAnswerResponse;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyEnabledResponse;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyOptionRequest;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyPopupStats;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyQuestionRequest;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyQuestionResponse;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyResponse;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyResultsResponse;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyUpdateRequest;
import com.seoulaim.domain.survey.dto.SurveyDashboardStats;
import com.seoulaim.domain.survey.dto.SurveyInvite;
import com.seoulaim.domain.survey.entity.SurveyOption;
import com.seoulaim.domain.survey.entity.SurveyQuestion;
import com.seoulaim.domain.survey.entity.SurveyQuestionType;
import com.seoulaim.domain.survey.entity.SurveyResponse;
import com.seoulaim.domain.survey.entity.SurveySettings;
import com.seoulaim.domain.survey.repository.SurveyOptionRepository;
import com.seoulaim.domain.survey.repository.SurveyPopupStateRepository;
import com.seoulaim.domain.survey.repository.SurveyQuestionRepository;
import com.seoulaim.domain.survey.repository.SurveyResponseRepository;
import com.seoulaim.domain.survey.repository.SurveySettingsRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 어드민 만족도 조사: 설문 설정(문구·질문) 저장, 팝업 켜고 끄기, 결과 조회, 기프티콘 발송 표시.
 *
 * <p>질문 id 는 바뀌지 않는다. 응답이 있는 질문은 삭제·형식 변경할 수 없고 숨김(archived)만 가능하다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminSurveyService {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final String DEFAULT_LANGUAGE = "en";
  private static final int MIN_CHOICE_OPTIONS = 2;

  private final SurveySettingsRepository surveySettingsRepository;
  private final SurveyQuestionRepository surveyQuestionRepository;
  private final SurveyOptionRepository surveyOptionRepository;
  private final SurveyPopupStateRepository surveyPopupStateRepository;
  private final SurveyResponseRepository surveyResponseRepository;
  private final PlanService planService;
  private final Clock clock;

  public AdminSurveyResponse getSurvey() {
    SurveySettings settings = getSettings();
    List<SurveyQuestion> questions =
        surveyQuestionRepository.findAllByOrderByDisplayOrderAscIdAsc();
    Map<String, Long> answerCounts = answerCounts();
    Map<String, Long> countsOfQuestions = new LinkedHashMap<>();
    questions.forEach(
        q ->
            countsOfQuestions.put(
                q.getId().toString(), answerCounts.getOrDefault(q.getId().toString(), 0L)));
    return new AdminSurveyResponse(
        settings.isEnabled(),
        new SurveyInvite(settings.getInviteTitle(), settings.getInviteBody()),
        toResponses(questions),
        settings.getUpdatedAt(),
        countsOfQuestions);
  }

  /**
   * 설문 전체 저장. 요청 순서 = display_order, id 없는 질문·보기는 새로 만든다.
   *
   * <ul>
   *   <li>빠진 기존 질문: 응답이 없으면 삭제, 있으면 409 SURVEY_QUESTION_HAS_ANSWERS
   *   <li>응답이 있는 질문의 형식 변경, 응답에 쓰인 보기 삭제도 409
   *   <li>en 문구가 비었거나 SINGLE/MULTIPLE 보기가 2개 미만이면 400 SURVEY_INVALID
   * </ul>
   */
  @Transactional
  public AdminSurveyResponse updateSurvey(AdminSurveyUpdateRequest request) {
    validate(request);

    Map<UUID, SurveyQuestion> existing =
        surveyQuestionRepository.findAll().stream()
            .collect(Collectors.toMap(SurveyQuestion::getId, Function.identity()));
    Map<UUID, List<SurveyOption>> existingOptions = optionsByQuestionId(existing.keySet());
    Map<String, Long> answerCounts = answerCounts();

    Set<UUID> keptIds = new HashSet<>();
    for (AdminSurveyQuestionRequest q : request.questions()) {
      if (q.id() == null) {
        continue;
      }
      SurveyQuestion question = existing.get(q.id());
      if (question == null) {
        throw new BusinessException(
            ErrorCode.SURVEY_QUESTION_NOT_FOUND, Map.of("questionId", q.id().toString()));
      }
      keptIds.add(q.id());
      boolean answered = answerCounts.getOrDefault(q.id().toString(), 0L) > 0;
      if (answered && question.getType() != q.type()) {
        throw hasAnswers(q.id(), "응답이 있는 질문은 형식을 바꿀 수 없습니다");
      }
      Set<UUID> ownOptionIds =
          existingOptions.getOrDefault(q.id(), List.of()).stream()
              .map(SurveyOption::getId)
              .collect(Collectors.toSet());
      Set<UUID> requestedOptionIds = q.type().hasOptions() ? optionIds(q) : Set.of();
      if (!ownOptionIds.containsAll(requestedOptionIds)) {
        throw invalid(q.id().toString(), "이 질문의 보기가 아닌 id 가 있습니다");
      }
      if (answered) {
        for (UUID removed : ownOptionIds) {
          if (!requestedOptionIds.contains(removed)
              && surveyResponseRepository.countOptionAnswers(q.id().toString(), removed.toString())
                  > 0) {
            throw hasAnswers(q.id(), "응답에 쓰인 보기는 삭제할 수 없습니다");
          }
        }
      }
    }

    List<SurveyQuestion> dropped =
        existing.values().stream().filter(q -> !keptIds.contains(q.getId())).toList();
    for (SurveyQuestion question : dropped) {
      if (answerCounts.getOrDefault(question.getId().toString(), 0L) > 0) {
        throw hasAnswers(question.getId(), "응답이 있는 질문은 숨김 처리해주세요");
      }
    }

    // 검증이 끝난 뒤에만 쓴다
    List<SurveyOption> droppedOptions =
        dropped.stream()
            .flatMap(q -> existingOptions.getOrDefault(q.getId(), List.of()).stream())
            .toList();
    surveyOptionRepository.deleteAllInBatch(droppedOptions);
    surveyQuestionRepository.deleteAllInBatch(dropped);

    List<AdminSurveyQuestionRequest> questions = request.questions();
    for (int i = 0; i < questions.size(); i++) {
      AdminSurveyQuestionRequest q = questions.get(i);
      SurveyQuestion question;
      if (q.id() == null) {
        question =
            surveyQuestionRepository.save(
                SurveyQuestion.create(q.type(), q.title(), q.required(), q.archived(), i));
      } else {
        question = existing.get(q.id());
        question.update(q.type(), q.title(), q.required(), q.archived(), i);
      }
      saveOptions(question, q, existingOptions.getOrDefault(question.getId(), List.of()));
    }

    getSettings().updateInvite(request.invite().title(), request.invite().body());
    surveySettingsRepository.touch(clock.instant());
    return getSurvey();
  }

  @Transactional
  public AdminSurveyEnabledResponse setEnabled(boolean enabled) {
    SurveySettings settings = getSettings();
    settings.changeEnabled(enabled);
    return new AdminSurveyEnabledResponse(settings.isEnabled());
  }

  /** from / to: KST 날짜 (포함). null 이면 그쪽은 열린 구간. */
  public AdminSurveyResultsResponse getResults(LocalDate from, LocalDate to) {
    if (from != null && to != null && from.isAfter(to)) {
      throw new BusinessException(
          ErrorCode.COMMON_INVALID_REQUEST, Map.of("from", "from 은 to 보다 늦을 수 없습니다"));
    }
    Instant start = from == null ? Instant.EPOCH : from.atStartOfDay(KST).toInstant();
    Instant end =
        to == null
            ? Instant.parse("9999-12-31T00:00:00Z")
            : to.plusDays(1).atStartOfDay(KST).toInstant();

    List<AdminSurveyAnswerResponse> responses =
        surveyResponseRepository
            .findAllBySubmittedAtGreaterThanEqualAndSubmittedAtLessThanOrderBySubmittedAtDescIdDesc(
                start, end)
            .stream()
            .map(AdminSurveyAnswerResponse::from)
            .toList();
    SurveyPopupStateRepository.PopupTotals totals = surveyPopupStateRepository.sumCounts();
    AdminSurveyPopupStats popup =
        new AdminSurveyPopupStats(
            planService.countPlanCreators(),
            surveyResponseRepository.count(),
            totals.getShown(),
            totals.getClosed(),
            totals.getAccepted(),
            totals.getSnoozed());
    return new AdminSurveyResultsResponse(
        toResponses(surveyQuestionRepository.findAllByOrderByDisplayOrderAscIdAsc()),
        responses,
        popup);
  }

  /** 기프티콘 발송 완료 표시. 이미 SENT 이거나 없는 id 는 건너뛰고, 바뀐 응답만 최신순으로 돌려준다. */
  @Transactional
  public List<AdminSurveyAnswerResponse> markGiftsSent(List<UUID> responseIds) {
    Instant now = clock.instant();
    List<SurveyResponse> updated = new ArrayList<>();
    for (SurveyResponse response :
        surveyResponseRepository.findAllById(responseIds.stream().distinct().toList())) {
      if (response.isGiftSent()) {
        continue;
      }
      response.markGiftSent(now);
      updated.add(response);
    }
    return updated.stream()
        .sorted(
            Comparator.comparing(SurveyResponse::getSubmittedAt)
                .thenComparing(SurveyResponse::getId)
                .reversed())
        .map(AdminSurveyAnswerResponse::from)
        .toList();
  }

  private void saveOptions(
      SurveyQuestion question, AdminSurveyQuestionRequest request, List<SurveyOption> existing) {
    List<AdminSurveyOptionRequest> requested =
        question.getType().hasOptions() ? request.optionsOrEmpty() : List.of();
    Set<UUID> keep =
        requested.stream()
            .map(AdminSurveyOptionRequest::id)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    surveyOptionRepository.deleteAllInBatch(
        existing.stream().filter(option -> !keep.contains(option.getId())).toList());

    Map<UUID, SurveyOption> byId =
        existing.stream().collect(Collectors.toMap(SurveyOption::getId, Function.identity()));
    for (int j = 0; j < requested.size(); j++) {
      AdminSurveyOptionRequest o = requested.get(j);
      if (o.id() == null) {
        surveyOptionRepository.save(SurveyOption.create(question.getId(), o.label(), j));
      } else {
        byId.get(o.id()).update(o.label(), j);
      }
    }
  }

  private void validate(AdminSurveyUpdateRequest request) {
    if (isBlankEn(request.invite().title())) {
      throw invalid("invite.title", "영어 제목이 필요합니다");
    }
    if (isBlankEn(request.invite().body())) {
      throw invalid("invite.body", "영어 본문이 필요합니다");
    }
    Set<UUID> questionIds = new HashSet<>();
    Set<UUID> optionIds = new HashSet<>();
    for (int i = 0; i < request.questions().size(); i++) {
      AdminSurveyQuestionRequest q = request.questions().get(i);
      String field = "questions[" + i + "]";
      if (q == null || q.type() == null) {
        throw invalid(field + ".type", "RATING / SINGLE / MULTIPLE / TEXT 중 하나여야 합니다");
      }
      if (isBlankEn(q.title())) {
        throw invalid(field + ".title", "영어 제목이 필요합니다");
      }
      if (q.id() != null && !questionIds.add(q.id())) {
        throw invalid(field + ".id", "같은 질문 id 가 두 번 있습니다");
      }
      if (!q.type().hasOptions()) {
        continue;
      }
      List<AdminSurveyOptionRequest> options = q.optionsOrEmpty();
      if (options.size() < MIN_CHOICE_OPTIONS) {
        throw invalid(field + ".options", "보기가 2개 이상 필요합니다");
      }
      for (int j = 0; j < options.size(); j++) {
        AdminSurveyOptionRequest o = options.get(j);
        if (o == null || isBlankEn(o.label())) {
          throw invalid(field + ".options[" + j + "].label", "영어 보기 문구가 필요합니다");
        }
        if (o.id() != null && !optionIds.add(o.id())) {
          throw invalid(field + ".options[" + j + "].id", "같은 보기 id 가 두 번 있습니다");
        }
      }
    }
  }

  private static Set<UUID> optionIds(AdminSurveyQuestionRequest q) {
    return q.optionsOrEmpty().stream()
        .map(AdminSurveyOptionRequest::id)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private static boolean isBlankEn(Map<String, String> text) {
    if (text == null) {
      return true;
    }
    String en = text.get(DEFAULT_LANGUAGE);
    return en == null || en.isBlank();
  }

  private List<AdminSurveyQuestionResponse> toResponses(List<SurveyQuestion> questions) {
    Map<UUID, List<SurveyOption>> options =
        optionsByQuestionId(questions.stream().map(SurveyQuestion::getId).toList());
    return questions.stream()
        .map(q -> AdminSurveyQuestionResponse.of(q, options.getOrDefault(q.getId(), List.of())))
        .toList();
  }

  private Map<UUID, List<SurveyOption>> optionsByQuestionId(java.util.Collection<UUID> ids) {
    if (ids.isEmpty()) {
      return Map.of();
    }
    return surveyOptionRepository.findAllByQuestionIdInOrderByDisplayOrderAscIdAsc(ids).stream()
        .collect(Collectors.groupingBy(SurveyOption::getQuestionId));
  }

  private Map<String, Long> answerCounts() {
    return surveyResponseRepository.countAnswersByQuestion().stream()
        .collect(Collectors.toMap(row -> (String) row[0], row -> ((Number) row[1]).longValue()));
  }

  private SurveySettings getSettings() {
    return surveySettingsRepository
        .findFirstByOrderByCreatedAtAsc()
        .orElseThrow(() -> new IllegalStateException("survey_settings 행이 없습니다"));
  }

  private static BusinessException invalid(String field, String reason) {
    return new BusinessException(ErrorCode.SURVEY_INVALID, Map.of(field, reason));
  }

  private static BusinessException hasAnswers(UUID questionId, String reason) {
    return new BusinessException(
        ErrorCode.SURVEY_QUESTION_HAS_ANSWERS, Map.of(questionId.toString(), reason));
  }

  /** 어드민 대시보드: 별점 평균(전체 기간)·최근 7일 응답 수·[from, to) 응답 수. */
  public SurveyDashboardStats getDashboardStats(Instant from, Instant to) {
    Instant now = clock.instant();
    Double averageRating =
        surveyQuestionRepository.findAllByArchivedFalseOrderByDisplayOrderAscIdAsc().stream()
            .filter(question -> question.getType() == SurveyQuestionType.RATING)
            .findFirst()
            .map(question -> surveyResponseRepository.averageRating(question.getId().toString()))
            .map(BigDecimal::doubleValue)
            .orElse(null);
    return new SurveyDashboardStats(
        averageRating,
        surveyResponseRepository.countBySubmittedAtGreaterThanEqualAndSubmittedAtLessThan(
            now.minus(Duration.ofDays(7)), now),
        surveyResponseRepository.countBySubmittedAtGreaterThanEqualAndSubmittedAtLessThan(
            from, to));
  }
}
