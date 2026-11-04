package com.seoulaim.domain.survey.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.plan.service.PlanService;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyAnswerResponse;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyOptionRequest;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyQuestionRequest;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyUpdateRequest;
import com.seoulaim.domain.survey.dto.SurveyInvite;
import com.seoulaim.domain.survey.entity.SurveyGiftStatus;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminSurveyServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-04T03:00:00Z");
  private static final SurveyInvite INVITE =
      new SurveyInvite(Map.of("en", "How was your plan?"), Map.of("en", "Gift card"));

  @Mock private SurveySettingsRepository surveySettingsRepository;
  @Mock private SurveyQuestionRepository surveyQuestionRepository;
  @Mock private SurveyOptionRepository surveyOptionRepository;
  @Mock private SurveyPopupStateRepository surveyPopupStateRepository;
  @Mock private SurveyResponseRepository surveyResponseRepository;
  @Mock private PlanService planService;

  private AdminSurveyService adminSurveyService;

  @BeforeEach
  void setUp() {
    adminSurveyService =
        new AdminSurveyService(
            surveySettingsRepository,
            surveyQuestionRepository,
            surveyOptionRepository,
            surveyPopupStateRepository,
            surveyResponseRepository,
            planService,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static AdminSurveyQuestionRequest text(UUID id, String en) {
    return new AdminSurveyQuestionRequest(
        id, SurveyQuestionType.TEXT, Map.of("en", en), false, false, List.of());
  }

  @Test
  @DisplayName("응답이 있는 질문을 목록에서 빼면 409 SURVEY_QUESTION_HAS_ANSWERS 이고 아무것도 지우지 않는다")
  void updateSurvey_응답있는질문삭제_409() {
    SurveyQuestion answered =
        SurveyQuestion.create(SurveyQuestionType.RATING, Map.of("en", "Rate"), true, false, 0);
    SurveyQuestion kept =
        SurveyQuestion.create(SurveyQuestionType.TEXT, Map.of("en", "Words"), false, false, 1);
    given(surveyQuestionRepository.findAll()).willReturn(List.of(answered, kept));
    given(surveyOptionRepository.findAllByQuestionIdInOrderByDisplayOrderAscIdAsc(anyCollection()))
        .willReturn(List.of());
    given(surveyResponseRepository.countAnswersByQuestion())
        .willReturn(List.<Object[]>of(new Object[] {answered.getId().toString(), 3L}));

    AdminSurveyUpdateRequest request =
        new AdminSurveyUpdateRequest(INVITE, List.of(text(kept.getId(), "Words")));

    assertThatThrownBy(() -> adminSurveyService.updateSurvey(request))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.SURVEY_QUESTION_HAS_ANSWERS);
    verify(surveyQuestionRepository, never()).deleteAllInBatch(anyList());
    verify(surveySettingsRepository, never()).touch(any());
  }

  @Test
  @DisplayName("응답이 없는 질문은 목록에서 빼면 보기와 함께 삭제된다")
  void updateSurvey_응답없는질문삭제_삭제() {
    SurveyQuestion unused =
        SurveyQuestion.create(SurveyQuestionType.SINGLE, Map.of("en", "Pick"), false, false, 0);
    SurveyOption a = SurveyOption.create(unused.getId(), Map.of("en", "a"), 0);
    SurveyOption b = SurveyOption.create(unused.getId(), Map.of("en", "b"), 1);
    given(surveyQuestionRepository.findAll()).willReturn(List.of(unused));
    given(surveyOptionRepository.findAllByQuestionIdInOrderByDisplayOrderAscIdAsc(anyCollection()))
        .willReturn(List.of(a, b));
    given(surveyResponseRepository.countAnswersByQuestion()).willReturn(List.of());
    given(surveyQuestionRepository.save(any(SurveyQuestion.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(surveySettingsRepository.findFirstByOrderByCreatedAtAsc())
        .willReturn(Optional.of(SurveySettings.createDefault()));

    adminSurveyService.updateSurvey(
        new AdminSurveyUpdateRequest(INVITE, List.of(text(null, "New"))));

    verify(surveyOptionRepository).deleteAllInBatch(List.of(a, b));
    verify(surveyQuestionRepository).deleteAllInBatch(List.of(unused));
    verify(surveySettingsRepository).touch(NOW);
  }

  @Test
  @DisplayName("SINGLE 보기가 2개 미만이거나 영어 제목이 비면 SURVEY_INVALID")
  void updateSurvey_보기부족_400() {
    AdminSurveyUpdateRequest fewOptions =
        new AdminSurveyUpdateRequest(
            INVITE,
            List.of(
                new AdminSurveyQuestionRequest(
                    null,
                    SurveyQuestionType.SINGLE,
                    Map.of("en", "Pick"),
                    false,
                    false,
                    List.of(new AdminSurveyOptionRequest(null, Map.of("en", "only"))))));
    AdminSurveyUpdateRequest noEnglish =
        new AdminSurveyUpdateRequest(INVITE, List.of(text(null, " ")));

    for (AdminSurveyUpdateRequest request : List.of(fewOptions, noEnglish)) {
      assertThatThrownBy(() -> adminSurveyService.updateSurvey(request))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ErrorCode.SURVEY_INVALID);
    }
    verify(surveyQuestionRepository, never()).findAll();
  }

  @Test
  @DisplayName("기프티콘 발송 표시는 이미 SENT 인 응답을 건너뛰고 바뀐 응답만 돌려준다")
  void markGiftsSent_이미발송_건너뜀() {
    SurveyResponse pending =
        SurveyResponse.create(UUID.randomUUID(), "a@test.com", Map.of(), Map.of(), NOW);
    SurveyResponse sent =
        SurveyResponse.create(UUID.randomUUID(), "b@test.com", Map.of(), Map.of(), NOW);
    Instant sentAt = Instant.parse("2026-11-01T00:00:00Z");
    sent.markGiftSent(sentAt);
    given(surveyResponseRepository.findAllById(anyList())).willReturn(List.of(pending, sent));

    List<AdminSurveyAnswerResponse> result =
        adminSurveyService.markGiftsSent(List.of(pending.getId(), sent.getId()));

    assertThat(result).extracting(AdminSurveyAnswerResponse::id).containsExactly(pending.getId());
    assertThat(result.getFirst().gift().status()).isEqualTo(SurveyGiftStatus.SENT);
    assertThat(result.getFirst().gift().sentAt()).isEqualTo(NOW);
    assertThat(sent.getGiftSentAt()).isEqualTo(sentAt);
  }
}
