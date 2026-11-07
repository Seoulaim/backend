package com.seoulaim.domain.diary.service;

import com.seoulaim.domain.diary.admin.dto.AdminPaymentDetailResponse;
import com.seoulaim.domain.diary.admin.dto.AdminPaymentPageResponse;
import com.seoulaim.domain.diary.admin.dto.AdminPaymentResponse;
import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryPayment;
import com.seoulaim.domain.diary.entity.DiaryPaymentStatus;
import com.seoulaim.domain.diary.repository.AdminPaymentQueryRepository;
import com.seoulaim.domain.diary.repository.AdminPaymentQueryRepository.Filter;
import com.seoulaim.domain.diary.repository.AdminPaymentQueryRepository.StatusTotal;
import com.seoulaim.domain.diary.repository.DiaryPaymentRepository;
import com.seoulaim.domain.diary.repository.DiaryRepository;
import com.seoulaim.domain.inquiry.dto.PaymentRefundRequest;
import com.seoulaim.domain.inquiry.service.RefundRequestQueryService;
import com.seoulaim.domain.order.gateway.PaymentCurrency;
import com.seoulaim.domain.user.dto.UserAccountInfo;
import com.seoulaim.domain.user.service.UserService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.util.AdminPaging;
import com.seoulaim.global.util.LikePattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 어드민 결제 관리 (다이어리 결제 전체).
 *
 * <p>회원 정보는 UserService, 환불 요청은 RefundRequestQueryService 경유 (다른 도메인 저장소를 직접 쓰지 않는다).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPaymentService {

  static final String PROVIDER = "toss";
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final AdminPaymentQueryRepository adminPaymentQueryRepository;
  private final DiaryPaymentRepository diaryPaymentRepository;
  private final DiaryRepository diaryRepository;
  private final UserService userService;
  private final RefundRequestQueryService refundRequestQueryService;

  /**
   * @param status pending | paid | failed | refunded (대소문자 무시). null·빈 값이면 전체
   * @param keyword 주문번호·이메일·닉네임 부분 일치 (대소문자 무시)
   * @param from 생성일(KST) 이상
   * @param to 생성일(KST) 이하
   */
  public AdminPaymentPageResponse getPayments(
      String status, String keyword, LocalDate from, LocalDate to, int page, int size) {
    AdminPaging.validate(page, size);
    if (from != null && to != null && from.isAfter(to)) {
      throw new BusinessException(
          ErrorCode.COMMON_INVALID_REQUEST, Map.of("from", "to 보다 늦을 수 없습니다"));
    }
    String trimmed = keyword == null ? "" : keyword.strip();
    String pattern = trimmed.isEmpty() ? null : LikePattern.containing(trimmed);
    Filter filter =
        new Filter(
            parseStatus(status),
            pattern,
            pattern == null ? List.of() : userService.searchUserIds(trimmed),
            from == null ? null : from.atStartOfDay(KST).toInstant(),
            to == null ? null : to.plusDays(1).atStartOfDay(KST).toInstant());

    List<DiaryPayment> payments = adminPaymentQueryRepository.search(filter, page, size);
    long total = adminPaymentQueryRepository.count(filter);
    return new AdminPaymentPageResponse(
        toResponses(payments), page, size, total, summary(filter.withoutStatus()));
  }

  public AdminPaymentDetailResponse getPayment(UUID paymentId) {
    DiaryPayment payment =
        diaryPaymentRepository
            .findById(paymentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    AdminPaymentResponse item = toResponses(List.of(payment)).getFirst();
    PaymentRefundRequest refundRequest =
        refundRequestQueryService.findLatestRefundRequests(List.of(paymentId)).get(paymentId);
    return new AdminPaymentDetailResponse(
        item,
        payment.getTossPaymentKey(),
        payment.getTossCancelKey(),
        payment.getFailureCode(),
        payment.getFailureMessage(),
        refundRequest == null
            ? null
            : new AdminPaymentDetailResponse.RefundRequest(
                refundRequest.inquiryId(), refundRequest.status()));
  }

  private List<AdminPaymentResponse> toResponses(List<DiaryPayment> payments) {
    if (payments.isEmpty()) {
      return List.of();
    }
    List<UUID> diaryIds = payments.stream().map(DiaryPayment::getDiaryId).distinct().toList();
    Map<UUID, Diary> diaries =
        diaryRepository.findAllByIdIn(diaryIds).stream()
            .collect(Collectors.toMap(Diary::getId, Function.identity()));
    Map<UUID, UserAccountInfo> members =
        userService
            .findAccounts(payments.stream().map(DiaryPayment::getUserId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(UserAccountInfo::id, Function.identity()));
    Map<UUID, PaymentRefundRequest> refundRequests =
        refundRequestQueryService.findLatestRefundRequests(
            payments.stream().map(DiaryPayment::getId).toList());
    Map<UUID, Long> paidPerDiary =
        diaryPaymentRepository
            .findAllByDiaryIdInAndStatus(diaryIds, DiaryPaymentStatus.PAID)
            .stream()
            .collect(Collectors.groupingBy(DiaryPayment::getDiaryId, Collectors.counting()));
    return payments.stream()
        .map(
            payment ->
                toResponse(
                    payment,
                    diaries.get(payment.getDiaryId()),
                    members.get(payment.getUserId()),
                    refundRequests.get(payment.getId()),
                    payment.isPaid()
                        ? (int) (paidPerDiary.getOrDefault(payment.getDiaryId(), 1L) - 1)
                        : 0))
        .toList();
  }

  private static AdminPaymentResponse toResponse(
      DiaryPayment payment,
      Diary diary,
      UserAccountInfo member,
      PaymentRefundRequest refundRequest,
      int sameDiaryPayments) {
    return new AdminPaymentResponse(
        payment.getId(),
        payment.getOrderNumber(),
        new AdminPaymentResponse.Member(
            payment.getUserId(),
            member == null ? null : member.nickname(),
            member == null ? null : member.email()),
        diary == null
            ? null
            : new AdminPaymentResponse.Diary(
                diary.getId(),
                diary.getName(),
                diary.isFree(),
                diary.pageCount(),
                diary.getCompletedAt(),
                diary.isDeleted()),
        payment.getAmount(),
        payment.getCurrency(),
        PROVIDER,
        apiStatus(payment.getStatus()),
        payment.getCreatedAt(),
        payment.getPaidAt(),
        payment.getRefundedAt(),
        payment.getRefundConsentAt(),
        payment.getEditedAt(),
        refundRequest != null && refundRequest.requested(),
        refundRequest == null ? null : refundRequest.inquiryId(),
        payment.getMethod(),
        payment.getMethodDetail(),
        sameDiaryPayments);
  }

  private AdminPaymentPageResponse.Summary summary(Filter filter) {
    List<StatusTotal> totals = adminPaymentQueryRepository.totalsByStatus(filter);
    Map<PaymentCurrency, BigDecimal> paidAmounts = amountsOf(totals, DiaryPaymentStatus.PAID);
    Map<PaymentCurrency, BigDecimal> refundedAmounts =
        amountsOf(totals, DiaryPaymentStatus.REFUNDED);
    return new AdminPaymentPageResponse.Summary(
        countOf(totals, DiaryPaymentStatus.PAID),
        krwAmount(paidAmounts),
        countOf(totals, DiaryPaymentStatus.REFUNDED),
        krwAmount(refundedAmounts),
        paidAmounts,
        refundedAmounts);
  }

  private static long countOf(List<StatusTotal> totals, DiaryPaymentStatus status) {
    return totals.stream()
        .filter(total -> total.status() == status)
        .mapToLong(StatusTotal::count)
        .sum();
  }

  private static Map<PaymentCurrency, BigDecimal> amountsOf(
      List<StatusTotal> totals, DiaryPaymentStatus status) {
    Map<PaymentCurrency, BigDecimal> amounts = new EnumMap<>(PaymentCurrency.class);
    totals.stream()
        .filter(total -> total.status() == status)
        .forEach(total -> amounts.merge(total.currency(), total.amount(), BigDecimal::add));
    return amounts;
  }

  private static long krwAmount(Map<PaymentCurrency, BigDecimal> amounts) {
    return amounts.getOrDefault(PaymentCurrency.KRW, BigDecimal.ZERO).longValueExact();
  }

  static String apiStatus(DiaryPaymentStatus status) {
    return status.name().toLowerCase(Locale.ROOT);
  }

  private static DiaryPaymentStatus parseStatus(String status) {
    if (status == null || status.isBlank()) {
      return null;
    }
    return Arrays.stream(DiaryPaymentStatus.values())
        .filter(value -> value.name().equalsIgnoreCase(status.strip()))
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST,
                    Map.of("status", "pending | paid | failed | refunded 중 하나여야 합니다")));
  }
}
