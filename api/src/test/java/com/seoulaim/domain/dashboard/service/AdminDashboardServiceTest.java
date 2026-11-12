package com.seoulaim.domain.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.dashboard.admin.dto.AdminDashboardResponse;
import com.seoulaim.domain.diary.dto.DiaryDashboardCounts;
import com.seoulaim.domain.diary.dto.PaymentDailyStat;
import com.seoulaim.domain.diary.entity.DiaryPaymentMethod;
import com.seoulaim.domain.diary.service.AdminPaymentService;
import com.seoulaim.domain.diary.service.DiaryService;
import com.seoulaim.domain.inquiry.admin.dto.AdminPendingRefundResponse;
import com.seoulaim.domain.inquiry.service.AdminInquiryService;
import com.seoulaim.domain.inquiry.service.RefundRequestService;
import com.seoulaim.domain.order.gateway.PaymentCurrency;
import com.seoulaim.domain.survey.dto.SurveyDashboardStats;
import com.seoulaim.domain.survey.service.AdminSurveyService;
import com.seoulaim.domain.user.service.UserService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminDashboardServiceTest {

  // UTC 2026-11-12 15:30 = KST 2026-11-13 00:30 → 오늘(KST)은 11-13
  private static final Instant NOW = Instant.parse("2026-11-12T15:30:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);

  @Mock private UserService userService;
  @Mock private DiaryService diaryService;
  @Mock private AdminPaymentService adminPaymentService;
  @Mock private AdminInquiryService adminInquiryService;
  @Mock private RefundRequestService refundRequestService;
  @Mock private AdminSurveyService adminSurveyService;

  private AdminDashboardService service;

  @BeforeEach
  void setUp() {
    service =
        new AdminDashboardService(
            userService,
            diaryService,
            adminPaymentService,
            adminInquiryService,
            refundRequestService,
            adminSurveyService,
            Clock.fixed(NOW, ZoneOffset.UTC));
    given(userService.countSignupsByDate(any(), any())).willReturn(Map.of());
    given(adminPaymentService.findPaidDailyStats(any(), any())).willReturn(List.of());
    given(adminPaymentService.findRefundedDailyStats(any(), any())).willReturn(List.of());
    given(adminPaymentService.getRecentPayments(anyInt())).willReturn(List.of());
    given(adminInquiryService.getPendingInquiries()).willReturn(List.of());
    given(adminInquiryService.getRecentInquiries(anyInt())).willReturn(List.of());
    given(refundRequestService.getPendingRefunds()).willReturn(List.of());
    given(refundRequestService.getRecentRefundRequests(anyInt())).willReturn(List.of());
    given(adminSurveyService.getDashboardStats(any(), any()))
        .willReturn(new SurveyDashboardStats(null, 0, 0));
    given(diaryService.countForDashboard(any(), any()))
        .willReturn(new DiaryDashboardCounts(0, 0, 0, 0, 0));
  }

  private static PaymentDailyStat stat(
      LocalDate date, DiaryPaymentMethod method, long count, String amount) {
    return new PaymentDailyStat(date, method.currency(), method, count, new BigDecimal(amount));
  }

  @Test
  @DisplayName("결제는 통화별로 따로 합산하고 원화 revenue 와 수단별 건수를 낸다")
  void getDashboard_통화별합계() {
    given(adminPaymentService.findPaidDailyStats(any(), any()))
        .willReturn(
            List.of(
                stat(TODAY, DiaryPaymentMethod.CARD, 2, "6000"),
                stat(TODAY, DiaryPaymentMethod.PAYPAL, 1, "2.99"),
                stat(TODAY.minusDays(1), DiaryPaymentMethod.PAYPAL, 2, "5.98")));
    given(adminPaymentService.findRefundedDailyStats(any(), any()))
        .willReturn(List.of(stat(TODAY, DiaryPaymentMethod.PAYPAL, 1, "2.99")));

    AdminDashboardResponse response = service.getDashboard(null);

    AdminDashboardResponse.Sales sales = response.sales();
    assertThat(sales.paidCount()).isEqualTo(5);
    assertThat(sales.paidAmounts())
        .containsExactly(
            Map.entry(PaymentCurrency.KRW, new BigDecimal("6000")),
            Map.entry(PaymentCurrency.USD, new BigDecimal("8.97")));
    assertThat(sales.refundedCount()).isEqualTo(1);
    assertThat(sales.refundedAmounts())
        .containsExactly(
            Map.entry(PaymentCurrency.KRW, BigDecimal.ZERO),
            Map.entry(PaymentCurrency.USD, new BigDecimal("2.99")));
    assertThat(sales.methodCounts())
        .containsExactly(
            Map.entry(DiaryPaymentMethod.CARD, 2L), Map.entry(DiaryPaymentMethod.PAYPAL, 3L));
    // 이번 달(11월): 원화 revenue 는 원화만, revenues 는 통화별
    assertThat(response.payments().thisMonth().paymentCount()).isEqualTo(5);
    assertThat(response.payments().thisMonth().revenue()).isEqualTo(6000);
    assertThat(response.payments().thisMonth().revenues().get(PaymentCurrency.USD))
        .isEqualByComparingTo("8.97");
    AdminDashboardResponse.DailySales todaySales = sales.daily().getLast();
    assertThat(todaySales.date()).isEqualTo(TODAY);
    assertThat(todaySales.paidCount()).isEqualTo(3);
    assertThat(todaySales.refundedAmounts().get(PaymentCurrency.USD)).isEqualByComparingTo("2.99");
  }

  @Test
  @DisplayName("날짜 경계는 KST 자정: UTC 15:30 이면 KST 다음 날을 오늘로 보고 그 기준으로 조회·분류한다")
  void getDashboard_KST자정경계() {
    LocalDate eightDaysAgo = TODAY.minusDays(7);
    given(userService.countSignupsByDate(any(), any()))
        .willReturn(Map.of(TODAY, 3L, TODAY.minusDays(1), 2L, eightDaysAgo, 5L));
    given(adminPaymentService.findPaidDailyStats(any(), any()))
        .willReturn(
            List.of(
                stat(eightDaysAgo, DiaryPaymentMethod.CARD, 1, "3000"),
                stat(LocalDate.of(2026, 10, 31), DiaryPaymentMethod.CARD, 4, "12000")));

    AdminDashboardResponse response = service.getDashboard("7d");

    // 14일 [10-31 00:00 KST, 11-14 00:00 KST)
    verify(userService)
        .countSignupsByDate(
            Instant.parse("2026-10-30T15:00:00Z"), Instant.parse("2026-11-13T15:00:00Z"));
    // 기간 7일 [11-07 00:00 KST, 11-14 00:00 KST)
    verify(adminPaymentService)
        .findRefundedDailyStats(
            Instant.parse("2026-11-06T15:00:00Z"), Instant.parse("2026-11-13T15:00:00Z"));
    verify(userService)
        .countWithdrawals(
            Instant.parse("2026-11-06T15:00:00Z"), Instant.parse("2026-11-13T15:00:00Z"));
    // 결제는 지난달 1일(10-01 00:00 KST)부터
    verify(adminPaymentService)
        .findPaidDailyStats(
            Instant.parse("2026-09-30T15:00:00Z"), Instant.parse("2026-11-13T15:00:00Z"));

    assertThat(response.from()).isEqualTo(LocalDate.of(2026, 11, 7));
    assertThat(response.to()).isEqualTo(TODAY);
    assertThat(response.signups()).isEqualTo(new AdminDashboardResponse.Signups(3, 2));
    // 8일 전 가입·결제는 7일 기간 밖, 14일 daily 안
    assertThat(response.members().newInRange()).isEqualTo(5);
    assertThat(response.sales().paidCount()).isZero();
    assertThat(response.daily()).hasSize(14);
    assertThat(response.daily().getFirst())
        .isEqualTo(new AdminDashboardResponse.Daily(LocalDate.of(2026, 10, 31), 0, 4));
    assertThat(response.daily().get(6))
        .isEqualTo(new AdminDashboardResponse.Daily(eightDaysAgo, 5, 1));
    // 10-31 결제는 지난달
    assertThat(response.payments().lastMonth().paymentCount()).isEqualTo(4);
    assertThat(response.payments().lastMonth().revenue()).isEqualTo(12000);
    assertThat(response.payments().thisMonth().paymentCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("데이터가 없으면 숫자는 0, 금액은 통화별 0, 목록은 빈 배열이고 일별 추이는 날짜만큼 0 으로 채운다")
  void getDashboard_빈데이터_0과빈배열() {
    AdminDashboardResponse response = service.getDashboard("30d");

    assertThat(response.range()).isEqualTo("30d");
    assertThat(response.signups()).isEqualTo(new AdminDashboardResponse.Signups(0, 0));
    assertThat(response.payments().thisMonth())
        .isEqualTo(
            new AdminDashboardResponse.MonthPayment(
                0,
                0,
                Map.of(
                    PaymentCurrency.KRW,
                    BigDecimal.ZERO,
                    PaymentCurrency.USD,
                    new BigDecimal("0.00"))));
    assertThat(response.daily()).hasSize(14).allMatch(d -> d.signups() == 0 && d.payments() == 0);
    assertThat(response.members().daily()).hasSize(30).allMatch(d -> d.signups() == 0);
    assertThat(response.sales().daily()).hasSize(30).allMatch(d -> d.paidCount() == 0);
    assertThat(response.sales().methodCounts())
        .containsExactly(
            Map.entry(DiaryPaymentMethod.CARD, 0L), Map.entry(DiaryPaymentMethod.PAYPAL, 0L));
    assertThat(response.pendingInquiries()).isEmpty();
    assertThat(response.pendingRefunds()).isEmpty();
    assertThat(response.pending()).isEqualTo(new AdminDashboardResponse.Pending(0, 0, 0, 0));
    assertThat(response.survey().averageRating()).isNull();
    assertThat(response.recent().payments()).isEmpty();
    assertThat(response.recent().inquiries()).isEmpty();
    assertThat(response.recent().refundRequests()).isEmpty();
  }

  @Test
  @DisplayName("처리 기한이 지난 환불은 overdue, 오늘·내일이면 dueSoon")
  void pending_기한임박과초과() {
    List<AdminPendingRefundResponse> refunds =
        List.of(
            refund(TODAY.minusDays(1)),
            refund(TODAY),
            refund(TODAY.plusDays(1)),
            refund(TODAY.plusDays(2)));

    assertThat(AdminDashboardService.pending(refunds, 3, TODAY))
        .isEqualTo(new AdminDashboardResponse.Pending(4, 2, 1, 3));
  }

  @Test
  @DisplayName("range 가 7d·30d·90d 가 아니면 400 COMMON_INVALID_REQUEST")
  void getDashboard_잘못된range_400() {
    assertThatThrownBy(() -> service.getDashboard("1y"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);
  }

  private static AdminPendingRefundResponse refund(LocalDate deadline) {
    return new AdminPendingRefundResponse(
        UUID.randomUUID(),
        "Refund",
        "Mina",
        new BigDecimal("3000"),
        PaymentCurrency.KRW,
        NOW,
        deadline);
  }
}
