package com.seoulaim.domain.dashboard.admin.dto;

import com.seoulaim.domain.diary.admin.dto.AdminPaymentResponse;
import com.seoulaim.domain.diary.entity.DiaryPaymentMethod;
import com.seoulaim.domain.inquiry.admin.dto.AdminInquiryResponse;
import com.seoulaim.domain.inquiry.admin.dto.AdminPendingInquiryResponse;
import com.seoulaim.domain.inquiry.admin.dto.AdminPendingRefundResponse;
import com.seoulaim.domain.inquiry.admin.dto.AdminRefundRequestResponse;
import com.seoulaim.domain.order.gateway.PaymentCurrency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 어드민 대시보드. 앞의 6개 필드는 어드민 mock(api/dashboard.js)과 같은 이름·구조이고, 나머지는 기간(range) 지표다. 날짜는 전부 KST.
 *
 * @param signups 오늘·어제 가입 수 (mock)
 * @param payments 이번 달·지난달 결제 (mock). KST 달력 월
 * @param daily 최근 14일 (오늘 포함, 오래된 순) 가입·결제 수 (mock)
 * @param pendingInquiries 답변 대기 문의 전체, 환불 제외, 최신순 (mock)
 * @param pendingRefunds 처리 대기 환불 전체, 최신순 (mock + currency·deadline)
 * @param survey 플랜 만족도 (mock + responsesInRange)
 * @param range 7d | 30d | 90d
 * @param from 기간 첫날 (포함)
 * @param to 기간 마지막 날 = 오늘 (포함)
 */
public record AdminDashboardResponse(
    Signups signups,
    MonthlyPayments payments,
    List<Daily> daily,
    List<AdminPendingInquiryResponse> pendingInquiries,
    List<AdminPendingRefundResponse> pendingRefunds,
    Survey survey,
    String range,
    LocalDate from,
    LocalDate to,
    Members members,
    Diaries diaries,
    Sales sales,
    Pending pending,
    Recent recent) {

  public record Signups(long today, long yesterday) {}

  public record MonthlyPayments(MonthPayment thisMonth, MonthPayment lastMonth) {}

  /**
   * @param paymentCount 그 달에 승인된 결제 수 (모든 통화, 이후 환불된 것 포함)
   * @param revenue 원화(KRW) 결제 금액 합계 (원). mock 은 원화만이라 하위 호환
   * @param revenues 통화별 합계 { KRW, USD } (두 통화 항상 포함)
   */
  public record MonthPayment(
      long paymentCount, long revenue, Map<PaymentCurrency, BigDecimal> revenues) {}

  /**
   * @param payments 그날 승인된 결제 수
   */
  public record Daily(LocalDate date, long signups, long payments) {}

  /**
   * @param averageRating 첫 별점 질문 전체 기간 평균. 없으면 null
   * @param responsesThisWeek 최근 7×24시간 응답 수 (mock 과 같은 기준)
   * @param responsesInRange 기간 내 응답 수
   */
  public record Survey(Double averageRating, long responsesThisWeek, long responsesInRange) {}

  /**
   * @param total 탈퇴하지 않은 회원 수 (정지 포함)
   * @param newInRange 기간 내 가입 (이후 탈퇴한 회원 포함)
   * @param withdrawnInRange 기간 내 탈퇴 (본인·강퇴)
   * @param daily 기간 일별 가입 수, 오래된 순
   */
  public record Members(
      long total, long newInRange, long withdrawnInRange, List<DailySignups> daily) {}

  public record DailySignups(LocalDate date, long signups) {}

  /** {@link com.seoulaim.domain.diary.dto.DiaryDashboardCounts} 참고. */
  public record Diaries(
      long total, long createdInRange, long finalized, long finalizedInRange, long paid) {}

  /**
   * 기간 내 결제. 승인은 승인 시각(paid_at), 환불은 환불 시각(refunded_at) 기준이라 같은 결제가 둘 다에 잡힐 수 있다.
   *
   * @param paidCount 승인 건수 (이후 환불된 것 포함)
   * @param paidAmounts 통화별 승인 금액 { KRW, USD }
   * @param refundedCount 환불 건수
   * @param refundedAmounts 통화별 환불 금액 { KRW, USD }
   * @param methodCounts 수단별 승인 건수 { CARD, PAYPAL }
   * @param daily 기간 일별, 오래된 순
   */
  public record Sales(
      long paidCount,
      Map<PaymentCurrency, BigDecimal> paidAmounts,
      long refundedCount,
      Map<PaymentCurrency, BigDecimal> refundedAmounts,
      Map<DiaryPaymentMethod, Long> methodCounts,
      List<DailySales> daily) {}

  public record DailySales(
      LocalDate date,
      long paidCount,
      Map<PaymentCurrency, BigDecimal> paidAmounts,
      long refundedCount,
      Map<PaymentCurrency, BigDecimal> refundedAmounts) {}

  /**
   * 지금 처리할 것 (기간 무관).
   *
   * @param refundRequests 처리 대기 환불 수
   * @param refundsDueSoon 그중 처리 기한이 오늘·내일인 것
   * @param refundsOverdue 그중 처리 기한이 지난 것
   * @param inquiries 답변 대기 문의 수 (환불 제외)
   */
  public record Pending(
      long refundRequests, long refundsDueSoon, long refundsOverdue, long inquiries) {}

  /** 최근 5건씩. 항목은 각 어드민 목록과 같은 형태. */
  public record Recent(
      List<AdminPaymentResponse> payments,
      List<AdminInquiryResponse> inquiries,
      List<AdminRefundRequestResponse> refundRequests) {}
}
