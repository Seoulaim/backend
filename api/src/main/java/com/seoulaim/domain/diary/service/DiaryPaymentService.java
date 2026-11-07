package com.seoulaim.domain.diary.service;

import com.seoulaim.domain.diary.dto.DiaryCheckoutResponse;
import com.seoulaim.domain.diary.dto.DiaryPaymentConfirmRequest;
import com.seoulaim.domain.diary.dto.DiaryPaymentConfirmResponse;
import com.seoulaim.domain.diary.dto.DiaryPaymentCreateRequest;
import com.seoulaim.domain.diary.dto.DiaryPaymentCreateResponse;
import com.seoulaim.domain.diary.dto.MyPaymentResponse;
import com.seoulaim.domain.diary.dto.PaymentRefundInfo;
import com.seoulaim.domain.diary.dto.RefundRequestCheck;
import com.seoulaim.domain.diary.dto.UserDiaryStats;
import com.seoulaim.domain.diary.dto.UserPaymentInfo;
import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryDownload;
import com.seoulaim.domain.diary.entity.DiaryPayment;
import com.seoulaim.domain.diary.entity.DiaryPaymentStatus;
import com.seoulaim.domain.diary.repository.DiaryDownloadRepository;
import com.seoulaim.domain.diary.repository.DiaryPaymentRepository;
import com.seoulaim.domain.diary.repository.DiaryRepository;
import com.seoulaim.domain.diary.repository.UserCount;
import com.seoulaim.domain.inquiry.service.RefundRequestQueryService;
import com.seoulaim.domain.order.gateway.PaymentCancelResult;
import com.seoulaim.domain.order.gateway.PaymentGateway;
import com.seoulaim.domain.order.gateway.PaymentResult;
import com.seoulaim.global.config.DiaryProperties;
import com.seoulaim.global.config.TossPaymentProperties;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 다이어리 결제. 토스 승인은 order 도메인의 {@link PaymentGateway} 구현(토스)을 그대로 쓴다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryPaymentService {

  static final String ORDER_NAME = "Seoulaim Diary";
  // 네트워크 타임아웃 뒤 재시도처럼 토스에서는 이미 취소된 결제. 환불 완료로 본다
  static final String ALREADY_CANCELED_CODE = "ALREADY_CANCELED_PAYMENT";
  private static final List<DiaryPaymentStatus> HISTORY_STATUSES =
      List.of(DiaryPaymentStatus.PAID, DiaryPaymentStatus.REFUNDED);
  private static final DateTimeFormatter ORDER_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);
  private static final String ORDER_NUMBER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final int ORDER_NUMBER_RANDOM_LENGTH = 10;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final DiaryService diaryService;
  private final DiaryRepository diaryRepository;
  private final DiaryPaymentRepository diaryPaymentRepository;
  private final DiaryDownloadRepository diaryDownloadRepository;
  private final PaymentGateway paymentGateway;
  private final RefundRequestQueryService refundRequestQueryService;
  private final DiaryProperties diaryProperties;
  private final TossPaymentProperties tossPaymentProperties;
  private final Clock clock;

  public DiaryCheckoutResponse getCheckout(UUID userId, UUID diaryId) {
    Diary diary = diaryService.findMyDiary(userId, diaryId);
    if (diary.isPaid()) {
      throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PAID);
    }
    return DiaryCheckoutResponse.of(diary, diaryProperties.price());
  }

  /** 결제 대기 건을 만든다. 같은 다이어리의 이전 PENDING 결제는 FAILED 로 바꾼다. */
  @Transactional
  public DiaryPaymentCreateResponse createPayment(
      UUID userId, UUID diaryId, DiaryPaymentCreateRequest request) {
    Diary diary = diaryService.findMyDiary(userId, diaryId);
    if (!Boolean.TRUE.equals(request.refundConsent())) {
      throw new BusinessException(ErrorCode.PAYMENT_CONSENT_REQUIRED);
    }
    if (diary.isPaid()) {
      throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PAID);
    }
    diaryPaymentRepository
        .findAllByDiaryIdAndStatus(diary.getId(), DiaryPaymentStatus.PENDING)
        .forEach(DiaryPayment::supersede);

    Instant now = clock.instant();
    long amount = diaryProperties.price();
    DiaryPayment payment =
        diaryPaymentRepository.save(
            DiaryPayment.createPending(
                diary.getId(), userId, generateOrderNumber(now), amount, now));
    return new DiaryPaymentCreateResponse(
        payment.getId(),
        payment.getOrderNumber(),
        amount,
        ORDER_NAME,
        tossPaymentProperties.clientKey());
  }

  /**
   * 토스 결제 승인. 결제 행을 잠근 채 게이트웨이를 호출해 동시 요청이 두 번 승인하지 않게 한다.
   *
   * <p>승인 실패 시 FAILED 를 저장한 뒤 PAYMENT_FAILED 를 던지므로 BusinessException 에는 롤백하지 않는다. 다른
   * BusinessException 경로(404, 금액 불일치, 게이트웨이 오류)는 그 전에 쓰기가 없다.
   */
  @Transactional(noRollbackFor = BusinessException.class)
  public DiaryPaymentConfirmResponse confirm(
      UUID userId, UUID paymentId, DiaryPaymentConfirmRequest request) {
    DiaryPayment payment =
        diaryPaymentRepository
            .findWithLockByIdAndUserId(paymentId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    if (payment.getAmount().compareTo(BigDecimal.valueOf(request.amount())) != 0) {
      throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }
    if (payment.isPaid()) {
      return new DiaryPaymentConfirmResponse(payment.getDiaryId());
    }
    // 실패·대체된 결제는 재승인하지 않는다. 앱은 결제를 새로 만든다
    if (!payment.isPending()) {
      throw new BusinessException(ErrorCode.PAYMENT_FAILED);
    }

    PaymentResult result =
        paymentGateway.confirm(request.paymentKey(), payment.getOrderNumber(), payment.getAmount());

    switch (result) {
      case PaymentResult.Success success -> {
        payment.markPaid(
            success.paymentKey() == null ? request.paymentKey() : success.paymentKey(),
            success.approvedAt() == null ? clock.instant() : success.approvedAt(),
            success.methodDetail());
        diaryRepository
            .findById(payment.getDiaryId())
            .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND))
            .markPaid();
        return new DiaryPaymentConfirmResponse(payment.getDiaryId());
      }
      case PaymentResult.Failure failure -> {
        payment.markFailed(request.paymentKey(), failure.code(), failure.message());
        throw new BusinessException(
            ErrorCode.PAYMENT_FAILED, failureFields(failure.code(), failure.message()));
      }
    }
  }

  /** 결제 후 편집 이력을 남긴다 (환불 정책 판단용). PAID 결제가 없으면 아무것도 하지 않는다. */
  @Transactional
  public void markEdited(UUID userId, UUID diaryId) {
    Diary diary = diaryService.findMyDiary(userId, diaryId);
    diaryPaymentRepository
        .findFirstByDiaryIdAndStatusOrderByPaidAtDescIdDesc(diary.getId(), DiaryPaymentStatus.PAID)
        .ifPresent(payment -> payment.recordEdited(clock.instant()));
  }

  /**
   * 환불 문의 접수용: 본인의 PAID 결제면 지금 시점의 환불 정책 판정을 돌려준다. 없거나 남의 것이거나 PAID 가 아니면 empty.
   *
   * <p>다이어리가 soft delete 됐어도 결제는 환불 대상이다.
   */
  public Optional<RefundRequestCheck> checkRefundRequest(UUID userId, UUID paymentId) {
    return diaryPaymentRepository
        .findByIdAndUserId(paymentId, userId)
        .filter(DiaryPayment::isPaid)
        .map(
            payment -> {
              boolean finalized =
                  diaryRepository
                      .findById(payment.getDiaryId())
                      .map(Diary::isFinalized)
                      .orElse(false);
              return new RefundRequestCheck(
                  payment.getId(), payment.isWithinRefundPolicy(clock.instant(), finalized));
            });
  }

  /** PAID·REFUNDED 결제 내역 최신순. 처리 대기 환불 문의가 있는 결제는 refundable=false. */
  public List<MyPaymentResponse> getMyPayments(UUID userId) {
    List<DiaryPayment> payments =
        diaryPaymentRepository.findAllByUserIdAndStatusInOrderByPaidAtDescIdDesc(
            userId, HISTORY_STATUSES);
    if (payments.isEmpty()) {
      return List.of();
    }
    Map<UUID, Diary> diaries =
        diaryRepository
            .findAllByIdIn(payments.stream().map(DiaryPayment::getDiaryId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(Diary::getId, Function.identity()));
    Set<UUID> refundRequested =
        refundRequestQueryService.findRequestedPaymentIds(
            payments.stream().map(DiaryPayment::getId).toList());
    Instant now = clock.instant();
    return payments.stream()
        .map(
            payment ->
                MyPaymentResponse.of(
                    payment,
                    diaries.get(payment.getDiaryId()),
                    now,
                    refundRequested.contains(payment.getId())))
        .toList();
  }

  /** 환불 관리용 결제 정보 (다이어리·다운로드 이력·지금 시점 정책 판정 포함). 없는 id 는 빠진다. */
  public List<PaymentRefundInfo> findRefundInfos(Collection<UUID> paymentIds) {
    if (paymentIds.isEmpty()) {
      return List.of();
    }
    return toRefundInfos(diaryPaymentRepository.findAllById(paymentIds));
  }

  /** 어드민 회원 상세: 회원의 PAID·REFUNDED 결제 최신순. */
  public List<UserPaymentInfo> findPaymentsOfUserForAdmin(UUID userId) {
    return diaryPaymentRepository
        .findAllByUserIdAndStatusInOrderByPaidAtDescIdDesc(userId, HISTORY_STATUSES)
        .stream()
        .map(UserPaymentInfo::from)
        .toList();
  }

  /** 회원별 다이어리 수·PAID 결제 수. 없는 회원은 {@link UserDiaryStats#EMPTY}. */
  public Map<UUID, UserDiaryStats> findUserStats(Collection<UUID> userIds) {
    if (userIds.isEmpty()) {
      return Map.of();
    }
    List<UUID> ids = userIds.stream().distinct().toList();
    Map<UUID, Long> diaryCounts = toCountMap(diaryRepository.countVisibleByUserIds(ids));
    Map<UUID, Long> paidCounts =
        toCountMap(diaryPaymentRepository.countByUserIdsAndStatus(ids, DiaryPaymentStatus.PAID));
    return ids.stream()
        .collect(
            Collectors.toMap(
                Function.identity(),
                id ->
                    new UserDiaryStats(
                        diaryCounts.getOrDefault(id, 0L), paidCounts.getOrDefault(id, 0L))));
  }

  /**
   * 어드민 환불 승인: 토스 결제를 전액 취소하고 결제를 REFUNDED 로, 다이어리를 환불 정책 제3조 3·4항대로 처리한다.
   *
   * <ul>
   *   <li>첫(무료) 다이어리: paid=false 만. 꾸민 내용은 남는다
   *   <li>그 외: soft delete
   * </ul>
   *
   * <p>토스가 취소를 거절하면 아무것도 바꾸지 않고 PAYMENT_REFUND_FAILED(fields 에 토스 code/message). 결제 행을 잠가 동시 승인이 두
   * 번 취소하지 않게 한다.
   */
  @Transactional
  public PaymentRefundInfo refund(UUID paymentId, String cancelReason) {
    DiaryPayment payment =
        diaryPaymentRepository
            .findWithLockById(paymentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    if (!payment.isPaid()) {
      throw new BusinessException(
          ErrorCode.PAYMENT_REFUND_FAILED,
          Map.of("code", "NOT_PAID", "message", "결제 완료(PAID) 상태가 아닙니다: " + payment.getStatus()));
    }
    PaymentCancelResult result = paymentGateway.cancel(payment.getTossPaymentKey(), cancelReason);
    Instant now = clock.instant();
    switch (result) {
      case PaymentCancelResult.Success success ->
          payment.markRefunded(
              success.cancelKey(), success.canceledAt() == null ? now : success.canceledAt());
      case PaymentCancelResult.Failure failure when ALREADY_CANCELED_CODE.equals(failure.code()) ->
          payment.markRefunded(null, now);
      case PaymentCancelResult.Failure failure ->
          throw new BusinessException(
              ErrorCode.PAYMENT_REFUND_FAILED, failureFields(failure.code(), failure.message()));
    }

    Diary diary =
        diaryRepository
            .findById(payment.getDiaryId())
            .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));
    if (diary.isFree()) {
      diary.revokePaid();
    } else if (!diary.isDeleted()) {
      // TODO(#55): 삭제한 다이어리의 사진·완성 페이지 S3 객체 정리. 지금은 soft delete 만
      diary.delete(now);
    }
    return PaymentRefundInfo.of(
        payment,
        diary,
        diaryDownloadRepository.findAllByDiaryIdInOrderByCreatedAtDescIdDesc(
            List.of(diary.getId())),
        now);
  }

  private List<PaymentRefundInfo> toRefundInfos(List<DiaryPayment> payments) {
    List<UUID> diaryIds = payments.stream().map(DiaryPayment::getDiaryId).distinct().toList();
    Map<UUID, Diary> diaries =
        diaryRepository.findAllByIdIn(diaryIds).stream()
            .collect(Collectors.toMap(Diary::getId, Function.identity()));
    Map<UUID, List<DiaryDownload>> downloads =
        diaryDownloadRepository.findAllByDiaryIdInOrderByCreatedAtDescIdDesc(diaryIds).stream()
            .collect(Collectors.groupingBy(DiaryDownload::getDiaryId));
    Instant now = clock.instant();
    return payments.stream()
        .map(
            payment ->
                PaymentRefundInfo.of(
                    payment,
                    diaries.get(payment.getDiaryId()),
                    downloads.getOrDefault(payment.getDiaryId(), List.of()),
                    now))
        .toList();
  }

  private static Map<UUID, Long> toCountMap(List<UserCount> counts) {
    return counts.stream().collect(Collectors.toMap(UserCount::getUserId, UserCount::getCount));
  }

  // DIA-yyyyMMdd-10자리. 토스 orderId 규칙(6~64자, 영숫자·-·_)을 만족한다
  private static String generateOrderNumber(Instant now) {
    StringBuilder builder =
        new StringBuilder("DIA-").append(ORDER_DATE_FORMAT.format(now)).append('-');
    for (int i = 0; i < ORDER_NUMBER_RANDOM_LENGTH; i++) {
      builder.append(ORDER_NUMBER_CHARS.charAt(RANDOM.nextInt(ORDER_NUMBER_CHARS.length())));
    }
    return builder.toString();
  }

  private static Map<String, String> failureFields(String code, String message) {
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("code", code);
    if (message != null) {
      fields.put("message", message);
    }
    return fields;
  }
}
