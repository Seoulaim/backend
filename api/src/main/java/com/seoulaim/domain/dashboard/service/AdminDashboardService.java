package com.seoulaim.domain.dashboard.service;

import com.seoulaim.domain.dashboard.admin.dto.AdminDashboardResponse;
import com.seoulaim.domain.dashboard.admin.dto.AdminDashboardResponse.DailySales;
import com.seoulaim.domain.dashboard.admin.dto.AdminDashboardResponse.DailySignups;
import com.seoulaim.domain.dashboard.admin.dto.AdminDashboardResponse.MonthPayment;
import com.seoulaim.domain.diary.dto.DiaryDashboardCounts;
import com.seoulaim.domain.diary.dto.PaymentDailyStat;
import com.seoulaim.domain.diary.entity.DiaryPaymentMethod;
import com.seoulaim.domain.diary.service.AdminPaymentService;
import com.seoulaim.domain.diary.service.DiaryService;
import com.seoulaim.domain.inquiry.admin.dto.AdminPendingInquiryResponse;
import com.seoulaim.domain.inquiry.admin.dto.AdminPendingRefundResponse;
import com.seoulaim.domain.inquiry.service.AdminInquiryService;
import com.seoulaim.domain.inquiry.service.RefundRequestService;
import com.seoulaim.domain.order.gateway.PaymentCurrency;
import com.seoulaim.domain.survey.dto.SurveyDashboardStats;
import com.seoulaim.domain.survey.service.AdminSurveyService;
import com.seoulaim.domain.user.service.UserService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 어드민 대시보드 집계. 각 도메인 service 의 집계 메서드만 쓴다 (다른 도메인 저장소를 직접 쓰지 않는다).
 *
 * <p>날짜 경계는 전부 Asia/Seoul. 일별 추이는 날짜별 group by 쿼리 한 번으로 받고 빈 날은 0 으로 채운다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

  static final ZoneId KST = ZoneId.of("Asia/Seoul");

  /** mock 의 daily: 최근 14일 (오늘 포함). */
  static final int DAILY_DAYS = 14;

  static final int RECENT_SIZE = 5;

  private final UserService userService;
  private final DiaryService diaryService;
  private final AdminPaymentService adminPaymentService;
  private final AdminInquiryService adminInquiryService;
  private final RefundRequestService refundRequestService;
  private final AdminSurveyService adminSurveyService;
  private final Clock clock;

  public AdminDashboardResponse getDashboard(String rangeValue) {
    DashboardRange range = DashboardRange.parse(rangeValue);
    LocalDate today = LocalDate.now(clock.withZone(KST));
    LocalDate tomorrow = today.plusDays(1);
    LocalDate rangeStart = today.minusDays(range.days() - 1L);
    LocalDate dailyStart = today.minusDays(DAILY_DAYS - 1L);
    LocalDate thisMonthStart = today.withDayOfMonth(1);
    LocalDate lastMonthStart = thisMonthStart.minusMonths(1);
    Instant rangeFrom = startOf(rangeStart);
    Instant end = startOf(tomorrow);

    // 가입: 14일·기간 중 이른 날부터 한 번에
    Map<LocalDate, Long> signups =
        userService.countSignupsByDate(startOf(min(rangeStart, dailyStart)), end);
    // 승인: 기간·14일·지난달 1일 중 가장 이른 날부터 한 번에
    List<PaymentDailyStat> paid =
        adminPaymentService.findPaidDailyStats(
            startOf(min(min(rangeStart, dailyStart), lastMonthStart)), end);
    List<PaymentDailyStat> refunded = adminPaymentService.findRefundedDailyStats(rangeFrom, end);

    List<AdminPendingInquiryResponse> pendingInquiries = adminInquiryService.getPendingInquiries();
    List<AdminPendingRefundResponse> pendingRefunds = refundRequestService.getPendingRefunds();
    SurveyDashboardStats survey = adminSurveyService.getDashboardStats(rangeFrom, end);
    DiaryDashboardCounts diaries = diaryService.countForDashboard(rangeFrom, end);

    List<PaymentDailyStat> paidInRange = between(paid, rangeStart, tomorrow);
    return new AdminDashboardResponse(
        new AdminDashboardResponse.Signups(
            signups.getOrDefault(today, 0L), signups.getOrDefault(today.minusDays(1), 0L)),
        new AdminDashboardResponse.MonthlyPayments(
            monthPayment(between(paid, thisMonthStart, tomorrow)),
            monthPayment(between(paid, lastMonthStart, thisMonthStart))),
        days(dailyStart, tomorrow)
            .map(
                date ->
                    new AdminDashboardResponse.Daily(
                        date, signups.getOrDefault(date, 0L), countOf(onDate(paid, date))))
            .toList(),
        pendingInquiries,
        pendingRefunds,
        new AdminDashboardResponse.Survey(
            survey.averageRating(), survey.responsesThisWeek(), survey.responsesInRange()),
        range.value(),
        rangeStart,
        today,
        new AdminDashboardResponse.Members(
            userService.countMembers(),
            days(rangeStart, tomorrow).mapToLong(date -> signups.getOrDefault(date, 0L)).sum(),
            userService.countWithdrawals(rangeFrom, end),
            days(rangeStart, tomorrow)
                .map(date -> new DailySignups(date, signups.getOrDefault(date, 0L)))
                .toList()),
        new AdminDashboardResponse.Diaries(
            diaries.total(),
            diaries.createdInRange(),
            diaries.finalized(),
            diaries.finalizedInRange(),
            diaries.paid()),
        new AdminDashboardResponse.Sales(
            countOf(paidInRange),
            amountsOf(paidInRange),
            countOf(refunded),
            amountsOf(refunded),
            methodCounts(paidInRange),
            days(rangeStart, tomorrow)
                .map(
                    date -> {
                      List<PaymentDailyStat> paidOnDate = onDate(paid, date);
                      List<PaymentDailyStat> refundedOnDate = onDate(refunded, date);
                      return new DailySales(
                          date,
                          countOf(paidOnDate),
                          amountsOf(paidOnDate),
                          countOf(refundedOnDate),
                          amountsOf(refundedOnDate));
                    })
                .toList()),
        pending(pendingRefunds, pendingInquiries.size(), today),
        new AdminDashboardResponse.Recent(
            adminPaymentService.getRecentPayments(RECENT_SIZE),
            adminInquiryService.getRecentInquiries(RECENT_SIZE),
            refundRequestService.getRecentRefundRequests(RECENT_SIZE)));
  }

  /** 처리 기한이 지났으면 overdue, 오늘·내일이면 dueSoon. 기한은 환불 관리와 같다(영업일 3일). */
  static AdminDashboardResponse.Pending pending(
      List<AdminPendingRefundResponse> refunds, long inquiries, LocalDate today) {
    long overdue =
        refunds.stream()
            .filter(refund -> refund.deadline() != null && refund.deadline().isBefore(today))
            .count();
    long dueSoon =
        refunds.stream()
            .filter(
                refund ->
                    refund.deadline() != null
                        && !refund.deadline().isBefore(today)
                        && !refund.deadline().isAfter(today.plusDays(1)))
            .count();
    return new AdminDashboardResponse.Pending(refunds.size(), dueSoon, overdue, inquiries);
  }

  private static MonthPayment monthPayment(List<PaymentDailyStat> stats) {
    Map<PaymentCurrency, BigDecimal> amounts = amountsOf(stats);
    return new MonthPayment(
        countOf(stats), amounts.get(PaymentCurrency.KRW).longValueExact(), amounts);
  }

  /** 통화별 합계. 결제가 없는 통화도 0 으로 넣는다 (KRW 0, USD 0.00). */
  static Map<PaymentCurrency, BigDecimal> amountsOf(Collection<PaymentDailyStat> stats) {
    Map<PaymentCurrency, BigDecimal> amounts = new EnumMap<>(PaymentCurrency.class);
    for (PaymentCurrency currency : PaymentCurrency.values()) {
      amounts.put(currency, currency.normalize(BigDecimal.ZERO));
    }
    stats.forEach(stat -> amounts.merge(stat.currency(), stat.amount(), BigDecimal::add));
    return amounts;
  }

  private static Map<DiaryPaymentMethod, Long> methodCounts(Collection<PaymentDailyStat> stats) {
    Map<DiaryPaymentMethod, Long> counts = new EnumMap<>(DiaryPaymentMethod.class);
    for (DiaryPaymentMethod method : DiaryPaymentMethod.values()) {
      counts.put(method, 0L);
    }
    stats.forEach(stat -> counts.merge(stat.method(), stat.count(), Long::sum));
    return counts;
  }

  private static long countOf(Collection<PaymentDailyStat> stats) {
    return stats.stream().mapToLong(PaymentDailyStat::count).sum();
  }

  private static List<PaymentDailyStat> onDate(List<PaymentDailyStat> stats, LocalDate date) {
    return filter(stats, stat -> stat.date().equals(date));
  }

  /** [from, to) 날짜의 행. */
  private static List<PaymentDailyStat> between(
      List<PaymentDailyStat> stats, LocalDate from, LocalDate to) {
    return filter(stats, stat -> !stat.date().isBefore(from) && stat.date().isBefore(to));
  }

  private static List<PaymentDailyStat> filter(
      List<PaymentDailyStat> stats, Predicate<PaymentDailyStat> predicate) {
    return stats.stream().filter(predicate).toList();
  }

  /** [from, to) 날짜, 오래된 순. */
  private static Stream<LocalDate> days(LocalDate from, LocalDate to) {
    return from.datesUntil(to);
  }

  private static LocalDate min(LocalDate a, LocalDate b) {
    return a.isBefore(b) ? a : b;
  }

  private static Instant startOf(LocalDate date) {
    return date.atStartOfDay(KST).toInstant();
  }
}
