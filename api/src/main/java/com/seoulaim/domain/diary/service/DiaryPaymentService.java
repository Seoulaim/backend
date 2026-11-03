package com.seoulaim.domain.diary.service;

import com.seoulaim.domain.diary.dto.DiaryCheckoutResponse;
import com.seoulaim.domain.diary.dto.DiaryPaymentConfirmRequest;
import com.seoulaim.domain.diary.dto.DiaryPaymentConfirmResponse;
import com.seoulaim.domain.diary.dto.DiaryPaymentCreateRequest;
import com.seoulaim.domain.diary.dto.DiaryPaymentCreateResponse;
import com.seoulaim.domain.diary.dto.MyPaymentResponse;
import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryPayment;
import com.seoulaim.domain.diary.entity.DiaryPaymentStatus;
import com.seoulaim.domain.diary.repository.DiaryPaymentRepository;
import com.seoulaim.domain.diary.repository.DiaryRepository;
import com.seoulaim.domain.order.gateway.PaymentGateway;
import com.seoulaim.domain.order.gateway.PaymentResult;
import com.seoulaim.global.config.DiaryProperties;
import com.seoulaim.global.config.TossPaymentProperties;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
  private final PaymentGateway paymentGateway;
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
    if (request.amount() != payment.getAmount()) {
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
            success.approvedAt() == null ? clock.instant() : success.approvedAt());
        diaryRepository
            .findById(payment.getDiaryId())
            .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND))
            .markPaid();
        return new DiaryPaymentConfirmResponse(payment.getDiaryId());
      }
      case PaymentResult.Failure failure -> {
        payment.markFailed(request.paymentKey(), failure.code(), failure.message());
        throw new BusinessException(ErrorCode.PAYMENT_FAILED, failureFields(failure));
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

  /** PAID·REFUNDED 결제 내역 최신순. */
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
    Instant now = clock.instant();
    return payments.stream()
        .map(payment -> MyPaymentResponse.of(payment, diaries.get(payment.getDiaryId()), now))
        .toList();
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

  private static Map<String, String> failureFields(PaymentResult.Failure failure) {
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("code", failure.code());
    if (failure.message() != null) {
      fields.put("message", failure.message());
    }
    return fields;
  }
}
