package com.seoulaim.domain.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.seoulaim.domain.dashboard.admin.dto.AdminDashboardResponse;
import com.seoulaim.domain.dashboard.service.AdminDashboardService;
import com.seoulaim.domain.diary.dto.PaymentDailyStat;
import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryPayment;
import com.seoulaim.domain.diary.entity.DiaryPaymentMethod;
import com.seoulaim.domain.diary.repository.DiaryPaymentRepository;
import com.seoulaim.domain.diary.repository.DiaryRepository;
import com.seoulaim.domain.diary.service.AdminPaymentService;
import com.seoulaim.domain.order.gateway.PaymentCurrency;
import com.seoulaim.domain.survey.entity.SurveyQuestion;
import com.seoulaim.domain.survey.entity.SurveyQuestionType;
import com.seoulaim.domain.survey.entity.SurveyResponse;
import com.seoulaim.domain.survey.repository.SurveyQuestionRepository;
import com.seoulaim.domain.survey.repository.SurveyResponseRepository;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** 대시보드 집계 쿼리(네이티브 KST 날짜 group by, JPQL 조건 합계)를 실제 PostgreSQL 로 검증한다. */
@Tag("integration")
@SpringBootTest
@Testcontainers
class DashboardIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @Autowired private AdminPaymentService adminPaymentService;
  @Autowired private AdminDashboardService adminDashboardService;
  @Autowired private UserRepository userRepository;
  @Autowired private DiaryRepository diaryRepository;
  @Autowired private DiaryPaymentRepository diaryPaymentRepository;
  @Autowired private SurveyQuestionRepository surveyQuestionRepository;
  @Autowired private SurveyResponseRepository surveyResponseRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private User mina;
  private Diary diary;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("delete from inquiry_answers");
    jdbcTemplate.update("delete from inquiries");
    jdbcTemplate.update("delete from survey_responses");
    jdbcTemplate.update("delete from survey_options");
    jdbcTemplate.update("delete from survey_questions");
    jdbcTemplate.update("delete from diary_payments");
    jdbcTemplate.update("delete from diaries");
    jdbcTemplate.update("delete from users");
    mina = userRepository.save(User.createWithEmail("mina@example.com", "hash", "Mina"));
    Diary paid = Diary.create(mina.getId(), false);
    paid.markPaid();
    diary = diaryRepository.save(paid);
  }

  private DiaryPayment paid(
      String orderNumber, DiaryPaymentMethod method, String amount, String at) {
    DiaryPayment payment =
        DiaryPayment.createPending(
            diary.getId(),
            mina.getId(),
            orderNumber,
            method,
            new BigDecimal(amount),
            Instant.parse(at));
    payment.markPaid("pk_" + orderNumber, Instant.parse(at));
    return payment;
  }

  @Test
  @DisplayName("승인 결제를 KST 날짜·통화·수단별로 합산한다 (UTC 15:00 = KST 자정 경계, 환불된 결제도 승인일에 포함, 미승인 제외)")
  void findPaidDailyStats_KST날짜별_통화별() {
    DiaryPayment lateNight = paid("DIA-1", DiaryPaymentMethod.CARD, "3000", "2026-11-10T14:59:59Z");
    DiaryPayment midnight = paid("DIA-2", DiaryPaymentMethod.CARD, "3000", "2026-11-10T15:00:00Z");
    DiaryPayment paypal = paid("DIA-3", DiaryPaymentMethod.PAYPAL, "2.99", "2026-11-11T01:00:00Z");
    DiaryPayment paypalRefunded =
        paid("DIA-4", DiaryPaymentMethod.PAYPAL, "2.99", "2026-11-11T02:00:00Z");
    paypalRefunded.markRefunded("tx", Instant.parse("2026-11-11T03:00:00Z"));
    DiaryPayment pending =
        DiaryPayment.createPending(
            diary.getId(), mina.getId(), "DIA-5", 3000, Instant.parse("2026-11-11T04:00:00Z"));
    diaryPaymentRepository.saveAllAndFlush(
        List.of(lateNight, midnight, paypal, paypalRefunded, pending));

    // [2026-11-10 00:00 KST, 2026-11-12 00:00 KST)
    List<PaymentDailyStat> stats =
        adminPaymentService.findPaidDailyStats(
            Instant.parse("2026-11-09T15:00:00Z"), Instant.parse("2026-11-11T15:00:00Z"));

    assertThat(stats)
        .containsExactlyInAnyOrder(
            new PaymentDailyStat(
                LocalDate.of(2026, 11, 10),
                PaymentCurrency.KRW,
                DiaryPaymentMethod.CARD,
                1,
                new BigDecimal("3000")),
            new PaymentDailyStat(
                LocalDate.of(2026, 11, 11),
                PaymentCurrency.KRW,
                DiaryPaymentMethod.CARD,
                1,
                new BigDecimal("3000")),
            new PaymentDailyStat(
                LocalDate.of(2026, 11, 11),
                PaymentCurrency.USD,
                DiaryPaymentMethod.PAYPAL,
                2,
                new BigDecimal("5.98")));
    assertThat(
            adminPaymentService.findRefundedDailyStats(
                Instant.parse("2026-11-09T15:00:00Z"), Instant.parse("2026-11-11T15:00:00Z")))
        .containsExactly(
            new PaymentDailyStat(
                LocalDate.of(2026, 11, 11),
                PaymentCurrency.USD,
                DiaryPaymentMethod.PAYPAL,
                1,
                new BigDecimal("2.99")));
  }

  @Test
  @DisplayName("대시보드 전체 조회가 실제 DB 에서 동작한다 (오늘 가입·결제·다이어리·설문이 집계에 잡힌다)")
  void getDashboard_실제DB() {
    Instant now = Instant.now();
    diaryPaymentRepository.saveAndFlush(
        paid("DIA-NOW", DiaryPaymentMethod.PAYPAL, "2.99", now.toString()));
    Diary deleted = Diary.create(mina.getId(), true);
    deleted.delete(now);
    diaryRepository.saveAndFlush(deleted);
    // 숨긴 별점 질문은 건너뛰고 첫 번째 보이는 별점 질문의 평균 (답하지 않은 응답 제외)
    surveyQuestionRepository.save(
        SurveyQuestion.create(SurveyQuestionType.RATING, Map.of("en", "Old"), false, true, 0));
    SurveyQuestion rating =
        surveyQuestionRepository.save(
            SurveyQuestion.create(
                SurveyQuestionType.RATING, Map.of("en", "Rate"), false, false, 1));
    User jin = userRepository.save(User.createWithEmail("jin@example.com", "hash", "Jin"));
    User ara = userRepository.save(User.createWithEmail("ara@example.com", "hash", "Ara"));
    surveyResponseRepository.saveAllAndFlush(
        List.of(
            SurveyResponse.create(
                mina.getId(),
                "mina@example.com",
                Map.of(),
                Map.of(rating.getId().toString(), 5),
                now),
            SurveyResponse.create(
                jin.getId(),
                "jin@example.com",
                Map.of(),
                Map.of(rating.getId().toString(), 4),
                now),
            SurveyResponse.create(ara.getId(), "ara@example.com", Map.of(), Map.of(), now)));

    AdminDashboardResponse response = adminDashboardService.getDashboard("7d");

    assertThat(response.members().total()).isEqualTo(3);
    assertThat(response.members().newInRange()).isEqualTo(3);
    assertThat(response.signups().today()).isEqualTo(3);
    assertThat(response.diaries().total()).isEqualTo(1);
    assertThat(response.diaries().createdInRange()).isEqualTo(2);
    assertThat(response.diaries().paid()).isEqualTo(1);
    assertThat(response.sales().paidCount()).isEqualTo(1);
    assertThat(response.sales().paidAmounts().get(PaymentCurrency.USD))
        .isEqualByComparingTo("2.99");
    assertThat(response.payments().thisMonth().revenue()).isZero();
    assertThat(response.daily().getLast().payments()).isEqualTo(1);
    assertThat(response.recent().payments()).hasSize(1);
    assertThat(response.survey().averageRating()).isEqualTo(4.5);
    assertThat(response.survey().responsesThisWeek()).isEqualTo(3);
    assertThat(response.survey().responsesInRange()).isEqualTo(3);
  }
}
