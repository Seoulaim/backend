package com.seoulaim.domain.survey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.seoulaim.domain.plan.entity.PlanDay;
import com.seoulaim.domain.plan.entity.TravelPlan;
import com.seoulaim.domain.plan.repository.PlanDayRepository;
import com.seoulaim.domain.plan.repository.TravelPlanRepository;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyAnswerResponse;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyOptionRequest;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyQuestionRequest;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyQuestionResponse;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyResponse;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyResultsResponse;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyUpdateRequest;
import com.seoulaim.domain.survey.dto.SurveyEventType;
import com.seoulaim.domain.survey.dto.SurveyInvite;
import com.seoulaim.domain.survey.entity.SurveyGiftStatus;
import com.seoulaim.domain.survey.entity.SurveyQuestionType;
import com.seoulaim.domain.survey.service.AdminSurveyService;
import com.seoulaim.domain.survey.service.SurveyService;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.repository.UserRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** 어드민 설문 저장 → 회원 초대·응답 → 어드민 결과·기프티콘 흐름을 실제 DB 로 검증한다. */
@Tag("integration")
@SpringBootTest
@Testcontainers
class SurveyFlowIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  private static final SurveyInvite INVITE =
      new SurveyInvite(
          Map.of("en", "How was your plan?", "ko", "플랜은 어떠셨나요?"),
          Map.of("en", "Get an Americano gift card"));

  @Autowired private AdminSurveyService adminSurveyService;
  @Autowired private SurveyService surveyService;
  @Autowired private UserRepository userRepository;
  @Autowired private TravelPlanRepository travelPlanRepository;
  @Autowired private PlanDayRepository planDayRepository;

  @Test
  @DisplayName("설문 저장 → 두 번째 방문에 초대 → 응답 → 결과·삭제 제한·기프티콘 표시")
  void surveyFlow() {
    AdminSurveyResponse saved =
        adminSurveyService.updateSurvey(
            new AdminSurveyUpdateRequest(
                INVITE,
                List.of(
                    question(null, SurveyQuestionType.RATING, "Rate", true, List.of()),
                    question(
                        null,
                        SurveyQuestionType.SINGLE,
                        "Best?",
                        false,
                        List.of(option(null, "Places"), option(null, "Route"))),
                    question(null, SurveyQuestionType.TEXT, "Words", false, List.of()))));
    AdminSurveyQuestionResponse rating = saved.questions().get(0);
    AdminSurveyQuestionResponse single = saved.questions().get(1);
    AdminSurveyQuestionResponse text = saved.questions().get(2);
    assertThat(single.options())
        .extracting(o -> o.label().get("en"))
        .containsExactly("Places", "Route");
    assertThat(saved.invite().title()).containsEntry("ko", "플랜은 어떠셨나요?");
    assertThat(saved.answerCounts()).containsEntry(rating.id().toString(), 0L);

    adminSurveyService.setEnabled(true);
    UUID userId = saveUserWithPlan();
    UUID noPlanUser = saveUser();

    // 플랜 없는 회원은 방문이 세어지지 않는다
    assertThat(surveyService.recordTravelVisit(noPlanUser).surveyInvite()).isNull();
    // 첫 방문은 초대 없음, 두 번째부터 초대
    assertThat(surveyService.recordTravelVisit(userId).surveyInvite()).isNull();
    assertThat(surveyService.recordTravelVisit(userId).surveyInvite().title())
        .containsEntry("en", "How was your plan?");
    surveyService.recordEvent(userId, SurveyEventType.SHOWN);
    surveyService.recordEvent(userId, SurveyEventType.SNOOZED);
    assertThat(surveyService.recordTravelVisit(userId).surveyInvite()).isNull();

    UUID routeId = single.options().get(1).id();
    surveyService.submit(
        userId, Map.of(rating.id().toString(), 4, single.id().toString(), routeId.toString()));
    assertThat(surveyService.getPlanSurvey(userId).answered()).isTrue();
    assertThatThrownBy(() -> surveyService.submit(userId, Map.of(rating.id().toString(), 5)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.SURVEY_ALREADY_ANSWERED);

    AdminSurveyResponse afterAnswer = adminSurveyService.getSurvey();
    assertThat(afterAnswer.answerCounts())
        .containsEntry(rating.id().toString(), 1L)
        .containsEntry(single.id().toString(), 1L)
        .containsEntry(text.id().toString(), 0L);

    // 응답에 쓰인 보기(Route) 삭제 → 409
    assertThatThrownBy(
            () ->
                adminSurveyService.updateSurvey(
                    new AdminSurveyUpdateRequest(
                        INVITE,
                        List.of(
                            question(
                                rating.id(), SurveyQuestionType.RATING, "Rate", true, List.of()),
                            question(
                                single.id(),
                                SurveyQuestionType.SINGLE,
                                "Best?",
                                false,
                                List.of(
                                    option(single.options().get(0).id(), "Places"),
                                    option(null, "Map"))),
                            question(
                                text.id(), SurveyQuestionType.TEXT, "Words", false, List.of())))))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.SURVEY_QUESTION_HAS_ANSWERS);

    // 응답 없는 TEXT 질문 삭제, 쓰이지 않은 보기(Places) 삭제, 별점 질문 숨김 → 성공
    AdminSurveyResponse edited =
        adminSurveyService.updateSurvey(
            new AdminSurveyUpdateRequest(
                INVITE,
                List.of(
                    question(
                        single.id(),
                        SurveyQuestionType.SINGLE,
                        "Best?",
                        false,
                        List.of(option(routeId, "Route!"), option(null, "Map"))),
                    new AdminSurveyQuestionRequest(
                        rating.id(),
                        SurveyQuestionType.RATING,
                        Map.of("en", "Rate"),
                        true,
                        true,
                        List.of()))));
    assertThat(edited.questions())
        .extracting(AdminSurveyQuestionResponse::id)
        .containsExactly(single.id(), rating.id());
    assertThat(edited.questions().get(0).options())
        .extracting(o -> o.label().get("en"))
        .containsExactly("Route!", "Map");
    assertThat(edited.updatedAt()).isAfterOrEqualTo(saved.updatedAt());
    assertThat(surveyService.getPlanSurvey(userId).questions())
        .extracting(q -> q.id())
        .containsExactly(single.id());

    LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
    AdminSurveyResultsResponse results = adminSurveyService.getResults(today, today);
    assertThat(results.responses()).hasSize(1);
    AdminSurveyAnswerResponse response = results.responses().getFirst();
    assertThat(response.userId()).isEqualTo(userId);
    assertThat(response.plan()).containsEntry("dayCount", 2).containsEntry("placeCount", 0);
    assertThat(results.questions()).hasSize(2);
    assertThat(results.popup().planCreators()).isEqualTo(1);
    assertThat(results.popup().responded()).isEqualTo(1);
    assertThat(results.popup().shown()).isEqualTo(1);
    assertThat(results.popup().snoozed()).isEqualTo(1);
    assertThat(adminSurveyService.getResults(today.plusDays(1), null).responses()).isEmpty();

    List<AdminSurveyAnswerResponse> marked =
        adminSurveyService.markGiftsSent(List.of(response.id()));
    assertThat(marked).hasSize(1);
    assertThat(marked.getFirst().gift().status()).isEqualTo(SurveyGiftStatus.SENT);
    assertThat(adminSurveyService.markGiftsSent(List.of(response.id()))).isEmpty();
  }

  private UUID saveUser() {
    return userRepository
        .save(User.createWithEmail(UUID.randomUUID() + "@test.com", "hash", "tester"))
        .getId();
  }

  private UUID saveUserWithPlan() {
    User user =
        userRepository.save(User.createWithEmail(UUID.randomUUID() + "@test.com", "hash", "fan"));
    TravelPlan plan =
        travelPlanRepository.save(
            TravelPlan.create(user, LocalDate.parse("2026-12-01"), LocalDate.parse("2026-12-05")));
    planDayRepository.save(PlanDay.create(plan, 1, LocalDate.parse("2026-12-01")));
    planDayRepository.save(PlanDay.create(plan, 2, LocalDate.parse("2026-12-03")));
    return user.getId();
  }

  private static AdminSurveyQuestionRequest question(
      UUID id,
      SurveyQuestionType type,
      String en,
      boolean required,
      List<AdminSurveyOptionRequest> options) {
    return new AdminSurveyQuestionRequest(id, type, Map.of("en", en), required, false, options);
  }

  private static AdminSurveyOptionRequest option(UUID id, String en) {
    return new AdminSurveyOptionRequest(id, Map.of("en", en));
  }
}
