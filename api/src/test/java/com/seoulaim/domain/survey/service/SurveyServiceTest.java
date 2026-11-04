package com.seoulaim.domain.survey.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.plan.dto.PlanSnapshot;
import com.seoulaim.domain.plan.service.PlanService;
import com.seoulaim.domain.survey.dto.SurveyEventType;
import com.seoulaim.domain.survey.dto.TravelVisitResponse;
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
import com.seoulaim.domain.user.dto.UserResponse;
import com.seoulaim.domain.user.service.UserService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SurveyServiceTest {

  // 2026-11-04 12:00 KST
  private static final Instant NOW = Instant.parse("2026-11-04T03:00:00Z");
  private static final UUID USER_ID = UUID.randomUUID();
  private static final Map<String, String> INVITE_TITLE = Map.of("en", "How was your plan?");
  private static final Map<String, String> INVITE_BODY = Map.of("en", "Get a gift card");

  @Mock private SurveySettingsRepository surveySettingsRepository;
  @Mock private SurveyQuestionRepository surveyQuestionRepository;
  @Mock private SurveyOptionRepository surveyOptionRepository;
  @Mock private SurveyPopupStateRepository surveyPopupStateRepository;
  @Mock private SurveyResponseRepository surveyResponseRepository;
  @Mock private PlanService planService;
  @Mock private UserService userService;

  private SurveyService surveyService;

  @BeforeEach
  void setUp() {
    surveyService =
        new SurveyService(
            surveySettingsRepository,
            surveyQuestionRepository,
            surveyOptionRepository,
            surveyPopupStateRepository,
            surveyResponseRepository,
            planService,
            userService,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private void givenSettings(boolean enabled) {
    SurveySettings settings = SurveySettings.createDefault();
    settings.changeEnabled(enabled);
    settings.updateInvite(INVITE_TITLE, INVITE_BODY);
    given(surveySettingsRepository.findFirstByOrderByCreatedAtAsc())
        .willReturn(Optional.of(settings));
  }

  private void givenState(int travelVisits, Instant snoozedUntil) {
    SurveyPopupState state = mock(SurveyPopupState.class);
    given(state.getTravelVisits()).willReturn(travelVisits);
    if (travelVisits >= SurveyService.MIN_TRAVEL_VISITS) {
      given(state.isSnoozedAt(NOW)).willReturn(snoozedUntil != null && snoozedUntil.isAfter(NOW));
    }
    given(surveyPopupStateRepository.findById(USER_ID)).willReturn(Optional.of(state));
  }

  @Nested
  @DisplayName("TRAVEL 방문 시 초대 판단")
  class RecordTravelVisit {

    @Test
    @DisplayName("팝업이 꺼져 있으면 방문은 세지만 초대하지 않는다")
    void recordTravelVisit_비활성_초대없음() {
      given(planService.hasPlan(USER_ID)).willReturn(true);
      givenSettings(false);

      TravelVisitResponse response = surveyService.recordTravelVisit(USER_ID);

      assertThat(response.surveyInvite()).isNull();
      verify(surveyPopupStateRepository).incrementTravelVisits(USER_ID, NOW);
    }

    @Test
    @DisplayName("플랜이 없으면 방문을 세지 않고 초대하지 않는다")
    void recordTravelVisit_플랜없음_방문미기록() {
      given(planService.hasPlan(USER_ID)).willReturn(false);

      TravelVisitResponse response = surveyService.recordTravelVisit(USER_ID);

      assertThat(response.surveyInvite()).isNull();
      verify(surveyPopupStateRepository, never()).incrementTravelVisits(any(), any());
    }

    @Test
    @DisplayName("플랜을 만든 뒤 첫 방문이면 초대하지 않는다")
    void recordTravelVisit_방문1회_초대없음() {
      given(planService.hasPlan(USER_ID)).willReturn(true);
      givenSettings(true);
      givenState(1, null);

      assertThat(surveyService.recordTravelVisit(USER_ID).surveyInvite()).isNull();
    }

    @Test
    @DisplayName("이미 응답했으면 초대하지 않는다")
    void recordTravelVisit_응답함_초대없음() {
      given(planService.hasPlan(USER_ID)).willReturn(true);
      givenSettings(true);
      givenState(5, null);
      given(surveyResponseRepository.existsByUserId(USER_ID)).willReturn(true);

      assertThat(surveyService.recordTravelVisit(USER_ID).surveyInvite()).isNull();
    }

    @Test
    @DisplayName("오늘 하루 보지 않기를 누른 날(KST 자정 전)에는 초대하지 않는다")
    void recordTravelVisit_스누즈중_초대없음() {
      given(planService.hasPlan(USER_ID)).willReturn(true);
      givenSettings(true);
      givenState(3, Instant.parse("2026-11-04T15:00:00Z"));

      assertThat(surveyService.recordTravelVisit(USER_ID).surveyInvite()).isNull();
    }

    @Test
    @DisplayName("켜져 있고 플랜이 있고 두 번째 방문이며 미응답·스누즈 만료면 초대 문구를 돌려준다")
    void recordTravelVisit_정상_초대반환() {
      given(planService.hasPlan(USER_ID)).willReturn(true);
      givenSettings(true);
      givenState(2, Instant.parse("2026-11-03T15:00:00Z"));
      given(surveyResponseRepository.existsByUserId(USER_ID)).willReturn(false);

      TravelVisitResponse response = surveyService.recordTravelVisit(USER_ID);

      assertThat(response.surveyInvite()).isNotNull();
      assertThat(response.surveyInvite().title()).isEqualTo(INVITE_TITLE);
      assertThat(response.surveyInvite().body()).isEqualTo(INVITE_BODY);
    }
  }

  @Test
  @DisplayName("SNOOZED 이벤트는 다음 KST 자정까지 스누즈한다")
  void recordEvent_스누즈_다음KST자정() {
    surveyService.recordEvent(USER_ID, SurveyEventType.SNOOZED);

    verify(surveyPopupStateRepository).snooze(USER_ID, Instant.parse("2026-11-04T15:00:00Z"), NOW);
  }

  @Test
  @DisplayName("KST 자정 직전 스누즈는 바로 다음 자정까지, 자정 직후는 그다음 자정까지")
  void nextKstMidnight_경계_KST기준() {
    assertThat(SurveyService.nextKstMidnight(Instant.parse("2026-11-04T14:59:59Z")))
        .isEqualTo(Instant.parse("2026-11-04T15:00:00Z"));
    assertThat(SurveyService.nextKstMidnight(Instant.parse("2026-11-04T15:00:00Z")))
        .isEqualTo(Instant.parse("2026-11-05T15:00:00Z"));
  }

  @Nested
  @DisplayName("응답 검증")
  class Submit {

    private SurveyQuestion rating;
    private SurveyQuestion single;
    private SurveyQuestion multiple;
    private SurveyQuestion text;
    private SurveyOption singleA;
    private SurveyOption singleB;
    private SurveyOption multiA;
    private SurveyOption multiB;

    @BeforeEach
    void setUpQuestions() {
      rating = SurveyQuestion.create(SurveyQuestionType.RATING, Map.of("en", "R"), true, false, 0);
      single = SurveyQuestion.create(SurveyQuestionType.SINGLE, Map.of("en", "S"), false, false, 1);
      multiple =
          SurveyQuestion.create(SurveyQuestionType.MULTIPLE, Map.of("en", "M"), false, false, 2);
      text = SurveyQuestion.create(SurveyQuestionType.TEXT, Map.of("en", "T"), false, false, 3);
      singleA = SurveyOption.create(single.getId(), Map.of("en", "a"), 0);
      singleB = SurveyOption.create(single.getId(), Map.of("en", "b"), 1);
      multiA = SurveyOption.create(multiple.getId(), Map.of("en", "a"), 0);
      multiB = SurveyOption.create(multiple.getId(), Map.of("en", "b"), 1);

      given(surveyResponseRepository.existsByUserId(USER_ID)).willReturn(false);
      given(planService.findLatestPlanSnapshot(USER_ID))
          .willReturn(
              Optional.of(
                  new PlanSnapshot(
                      UUID.randomUUID(),
                      LocalDate.parse("2026-11-10"),
                      LocalDate.parse("2026-11-14"),
                      3,
                      9)));
      given(surveyQuestionRepository.findAllByArchivedFalseOrderByDisplayOrderAscIdAsc())
          .willReturn(List.of(rating, single, multiple, text));
      given(surveyOptionRepository.findAllByQuestionIdInOrderByDisplayOrderAscIdAsc(anyList()))
          .willReturn(List.of(singleA, singleB, multiA, multiB));
    }

    private Map<String, Object> answers(Object... pairs) {
      Map<String, Object> map = new HashMap<>();
      for (int i = 0; i < pairs.length; i += 2) {
        map.put(((SurveyQuestion) pairs[i]).getId().toString(), pairs[i + 1]);
      }
      return map;
    }

    private void assertInvalid(Map<String, Object> answers) {
      assertThatThrownBy(() -> surveyService.submit(USER_ID, answers))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.SURVEY_INVALID);
      verify(surveyResponseRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("필수 질문(별점)이 빠지면 SURVEY_INVALID")
    void submit_필수누락_400() {
      assertInvalid(answers(single, singleA.getId().toString(), text, "good"));
    }

    @Test
    @DisplayName("별점이 1~5 정수가 아니면 SURVEY_INVALID")
    void submit_별점범위밖_400() {
      assertInvalid(answers(rating, 6));
      assertInvalid(answers(rating, 4.5));
      assertInvalid(answers(rating, "5"));
    }

    @Test
    @DisplayName("SINGLE·MULTIPLE 에 그 질문의 보기가 아닌 id 를 주면 SURVEY_INVALID")
    void submit_다른질문보기_400() {
      assertInvalid(answers(rating, 5, single, multiA.getId().toString()));
      assertInvalid(answers(rating, 5, multiple, List.of(singleA.getId().toString())));
      assertInvalid(answers(rating, 5, multiple, multiA.getId().toString()));
    }

    @Test
    @DisplayName("글이 1000자를 넘거나 문자열이 아니면 SURVEY_INVALID")
    void submit_글형식오류_400() {
      assertInvalid(answers(rating, 5, text, "a".repeat(1001)));
      assertInvalid(answers(rating, 5, text, 42));
    }

    @Test
    @DisplayName("올바른 응답은 빈 값을 빼고 플랜 요약·계정 이메일과 함께 저장한다")
    void submit_정상_저장() {
      given(userService.getUserProfile(USER_ID))
          .willReturn(new UserResponse(USER_ID, "fan@test.com", "fan", null, "en"));

      surveyService.submit(
          USER_ID,
          answers(
              rating,
              4,
              single,
              singleB.getId().toString(),
              multiple,
              List.of(),
              text,
              "  great  "));

      ArgumentCaptor<SurveyResponse> captor = ArgumentCaptor.forClass(SurveyResponse.class);
      verify(surveyResponseRepository).saveAndFlush(captor.capture());
      SurveyResponse saved = captor.getValue();
      assertThat(saved.getEmail()).isEqualTo("fan@test.com");
      assertThat(saved.getSubmittedAt()).isEqualTo(NOW);
      assertThat(saved.getAnswers())
          .containsEntry(rating.getId().toString(), 4)
          .containsEntry(single.getId().toString(), singleB.getId().toString())
          .containsEntry(text.getId().toString(), "great")
          .doesNotContainKey(multiple.getId().toString());
      assertThat(saved.getPlanSnapshot())
          .containsEntry("startDate", "2026-11-10")
          .containsEntry("dayCount", 3)
          .containsEntry("placeCount", 9);
    }
  }

  @Test
  @DisplayName("이미 응답했으면 SURVEY_ALREADY_ANSWERED")
  void submit_이미응답_409() {
    given(surveyResponseRepository.existsByUserId(USER_ID)).willReturn(true);

    assertThatThrownBy(() -> surveyService.submit(USER_ID, Map.of()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.SURVEY_ALREADY_ANSWERED);
  }

  @Test
  @DisplayName("플랜이 없으면 설문 화면은 PLAN_NOT_FOUND")
  void getPlanSurvey_플랜없음_404() {
    given(planService.hasPlan(USER_ID)).willReturn(false);

    assertThatThrownBy(() -> surveyService.getPlanSurvey(USER_ID))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PLAN_NOT_FOUND);
  }
}
