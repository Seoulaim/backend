package com.seoulaim.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import java.time.Clock;
import java.time.Duration;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiaryPaymentServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-03T03:00:00Z");
  private static final UUID USER_ID = UUID.randomUUID();
  private static final long PRICE = 3000L;

  @Mock private DiaryService diaryService;
  @Mock private DiaryRepository diaryRepository;
  @Mock private DiaryPaymentRepository diaryPaymentRepository;
  @Mock private PaymentGateway paymentGateway;

  private DiaryPaymentService diaryPaymentService;

  @BeforeEach
  void setUp() {
    diaryPaymentService =
        new DiaryPaymentService(
            diaryService,
            diaryRepository,
            diaryPaymentRepository,
            paymentGateway,
            new DiaryProperties(PRICE, 2),
            new TossPaymentProperties("test_sk", "test_ck"),
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private Diary givenMyDiary(Diary diary) {
    given(diaryService.findMyDiary(USER_ID, diary.getId())).willReturn(diary);
    return diary;
  }

  private DiaryPayment givenLockedPayment(Diary diary) {
    DiaryPayment payment =
        DiaryPayment.createPending(diary.getId(), USER_ID, "DIA-20261103-ABCDE12345", PRICE, NOW);
    given(diaryPaymentRepository.findWithLockByIdAndUserId(payment.getId(), USER_ID))
        .willReturn(Optional.of(payment));
    return payment;
  }

  @Test
  @DisplayName("결제 생성 시 기존 PENDING 결제를 FAILED 로 바꾸고 새 PENDING 결제를 만든다")
  void createPayment_기존대기건_대체() {
    Diary diary = givenMyDiary(Diary.create(USER_ID, false));
    DiaryPayment old = DiaryPayment.createPending(diary.getId(), USER_ID, "DIA-OLD", PRICE, NOW);
    given(
            diaryPaymentRepository.findAllByDiaryIdAndStatus(
                diary.getId(), DiaryPaymentStatus.PENDING))
        .willReturn(List.of(old));
    given(diaryPaymentRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

    DiaryPaymentCreateResponse response =
        diaryPaymentService.createPayment(
            USER_ID, diary.getId(), new DiaryPaymentCreateRequest(null, true));

    assertThat(old.getStatus()).isEqualTo(DiaryPaymentStatus.FAILED);
    ArgumentCaptor<DiaryPayment> captor = ArgumentCaptor.forClass(DiaryPayment.class);
    verify(diaryPaymentRepository).save(captor.capture());
    DiaryPayment created = captor.getValue();
    assertThat(created.getStatus()).isEqualTo(DiaryPaymentStatus.PENDING);
    assertThat(created.getRefundConsentAt()).isEqualTo(NOW);
    assertThat(response.orderId()).isEqualTo(created.getId());
    assertThat(response.orderNumber()).matches("DIA-20261103-[A-Z0-9]{10}");
    assertThat(response.finalAmount()).isEqualTo(PRICE);
    assertThat(response.orderName()).isEqualTo("Seoulaim Diary");
    assertThat(response.tossClientKey()).isEqualTo("test_ck");
  }

  @Test
  @DisplayName("환불 규정 동의가 true 가 아니면 400 PAYMENT_CONSENT_REQUIRED")
  void createPayment_동의없음_400() {
    Diary diary = givenMyDiary(Diary.create(USER_ID, false));

    assertThatThrownBy(
            () ->
                diaryPaymentService.createPayment(
                    USER_ID, diary.getId(), new DiaryPaymentCreateRequest(null, null)))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PAYMENT_CONSENT_REQUIRED);
    verify(diaryPaymentRepository, never()).save(any());
  }

  @Test
  @DisplayName("이미 결제된 다이어리는 결제 생성·결제 화면 모두 409 PAYMENT_ALREADY_PAID")
  void createPayment_결제완료_409() {
    Diary diary = Diary.create(USER_ID, false);
    diary.markPaid();
    givenMyDiary(diary);

    assertThatThrownBy(
            () ->
                diaryPaymentService.createPayment(
                    USER_ID, diary.getId(), new DiaryPaymentCreateRequest(null, true)))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PAYMENT_ALREADY_PAID);
    assertThatThrownBy(() -> diaryPaymentService.getCheckout(USER_ID, diary.getId()))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PAYMENT_ALREADY_PAID);
  }

  @Test
  @DisplayName("승인 성공 시 결제는 PAID, 다이어리는 paid=true 가 된다")
  void confirm_성공_paid() {
    Diary diary = Diary.create(USER_ID, false);
    DiaryPayment payment = givenLockedPayment(diary);
    given(diaryRepository.findById(diary.getId())).willReturn(Optional.of(diary));
    Instant approvedAt = NOW.minusSeconds(5);
    given(paymentGateway.confirm("pk_1", payment.getOrderNumber(), PRICE))
        .willReturn(PaymentResult.success("pk_1", "카드", approvedAt, "{}"));

    DiaryPaymentConfirmResponse response =
        diaryPaymentService.confirm(
            USER_ID, payment.getId(), new DiaryPaymentConfirmRequest("pk_1", PRICE));

    assertThat(response.diaryId()).isEqualTo(diary.getId());
    assertThat(payment.getStatus()).isEqualTo(DiaryPaymentStatus.PAID);
    assertThat(payment.getPaidAt()).isEqualTo(approvedAt);
    assertThat(payment.getTossPaymentKey()).isEqualTo("pk_1");
    assertThat(diary.isPaid()).isTrue();
  }

  @Test
  @DisplayName("금액이 다르면 게이트웨이를 호출하지 않고 400 PAYMENT_AMOUNT_MISMATCH")
  void confirm_금액불일치_400() {
    DiaryPayment payment = givenLockedPayment(Diary.create(USER_ID, false));

    assertThatThrownBy(
            () ->
                diaryPaymentService.confirm(
                    USER_ID, payment.getId(), new DiaryPaymentConfirmRequest("pk_1", PRICE - 1)))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    verify(paymentGateway, never()).confirm(anyString(), anyString(), anyLong());
  }

  @Test
  @DisplayName("토스 승인 실패면 결제를 FAILED 로 저장하고 402 PAYMENT_FAILED 와 토스 코드를 내린다")
  void confirm_토스실패_402() {
    Diary diary = Diary.create(USER_ID, false);
    DiaryPayment payment = givenLockedPayment(diary);
    given(paymentGateway.confirm("pk_1", payment.getOrderNumber(), PRICE))
        .willReturn(PaymentResult.failure("REJECT_CARD_COMPANY", "카드사 거절", "{}"));

    assertThatThrownBy(
            () ->
                diaryPaymentService.confirm(
                    USER_ID, payment.getId(), new DiaryPaymentConfirmRequest("pk_1", PRICE)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            exception -> {
              BusinessException business = (BusinessException) exception;
              assertThat(business.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_FAILED);
              assertThat(business.getFields())
                  .isEqualTo(Map.of("code", "REJECT_CARD_COMPANY", "message", "카드사 거절"));
            });
    assertThat(payment.getStatus()).isEqualTo(DiaryPaymentStatus.FAILED);
    assertThat(diary.isPaid()).isFalse();
  }

  @Test
  @DisplayName("이미 PAID 인 결제에 다시 승인 요청하면 게이트웨이 호출 없이 200(diaryId)")
  void confirm_이미결제_멱등() {
    Diary diary = Diary.create(USER_ID, false);
    DiaryPayment payment = givenLockedPayment(diary);
    payment.markPaid("pk_1", NOW);

    DiaryPaymentConfirmResponse response =
        diaryPaymentService.confirm(
            USER_ID, payment.getId(), new DiaryPaymentConfirmRequest("pk_1", PRICE));

    assertThat(response.diaryId()).isEqualTo(diary.getId());
    verify(paymentGateway, never()).confirm(anyString(), anyString(), anyLong());
  }

  @Test
  @DisplayName("edited 는 최신 PAID 결제의 edited_at 이 비어 있을 때만 기록한다")
  void markEdited_최신PAID결제_기록() {
    Diary diary = givenMyDiary(Diary.create(USER_ID, false));
    DiaryPayment latest = DiaryPayment.createPending(diary.getId(), USER_ID, "DIA-1", PRICE, NOW);
    latest.markPaid("pk", NOW.minusSeconds(60));
    given(
            diaryPaymentRepository.findFirstByDiaryIdAndStatusOrderByPaidAtDescIdDesc(
                diary.getId(), DiaryPaymentStatus.PAID))
        .willReturn(Optional.of(latest));

    diaryPaymentService.markEdited(USER_ID, diary.getId());
    assertThat(latest.getEditedAt()).isEqualTo(NOW);

    // 두 번째 호출은 처음 시각을 유지한다
    latest.recordEdited(NOW.plusSeconds(100));
    assertThat(latest.getEditedAt()).isEqualTo(NOW);
  }

  @Test
  @DisplayName("PAID 결제가 없으면 edited 는 아무것도 하지 않는다")
  void markEdited_결제없음_무시() {
    Diary diary = givenMyDiary(Diary.create(USER_ID, true));
    given(
            diaryPaymentRepository.findFirstByDiaryIdAndStatusOrderByPaidAtDescIdDesc(
                diary.getId(), DiaryPaymentStatus.PAID))
        .willReturn(Optional.empty());

    diaryPaymentService.markEdited(USER_ID, diary.getId());

    verify(diaryPaymentRepository, never()).save(any());
  }

  @Test
  @DisplayName("결제 내역: 7일 이내·미완성이면 refundable, 편집 이력까지 없으면 withinPolicy. 삭제된 다이어리는 이름이 null")
  void getMyPayments_환불가능판단() {
    Diary editing = Diary.create(USER_ID, false);
    Diary finalized = Diary.create(USER_ID, false);
    finalized.finalizeWith(List.of(), Map.of(), NOW);
    Diary deleted = Diary.create(USER_ID, false);
    deleted.delete(NOW);

    DiaryPayment fresh = paid(editing, NOW.minus(Duration.ofDays(1)));
    DiaryPayment edited = paid(editing, NOW.minus(Duration.ofDays(2)));
    edited.recordEdited(NOW);
    DiaryPayment old = paid(editing, NOW.minus(Duration.ofDays(8)));
    DiaryPayment ofFinalized = paid(finalized, NOW.minus(Duration.ofDays(1)));
    DiaryPayment ofDeleted = paid(deleted, NOW.minus(Duration.ofDays(1)));
    given(
            diaryPaymentRepository.findAllByUserIdAndStatusInOrderByPaidAtDescIdDesc(
                USER_ID, List.of(DiaryPaymentStatus.PAID, DiaryPaymentStatus.REFUNDED)))
        .willReturn(List.of(fresh, edited, old, ofFinalized, ofDeleted));
    given(diaryRepository.findAllByIdIn(any())).willReturn(List.of(editing, finalized, deleted));

    List<MyPaymentResponse> responses = diaryPaymentService.getMyPayments(USER_ID);

    assertThat(responses)
        .extracting(MyPaymentResponse::refundable, MyPaymentResponse::withinPolicy)
        .containsExactly(
            tuple(true, true),
            tuple(true, false),
            tuple(false, false),
            tuple(false, false),
            tuple(true, true));
    assertThat(responses.get(0).name()).isEqualTo(Diary.DEFAULT_NAME);
    assertThat(responses.get(4).name()).isNull();
    assertThat(responses.get(4).thumbnailKey()).isNull();
  }

  private static DiaryPayment paid(Diary diary, Instant paidAt) {
    DiaryPayment payment =
        DiaryPayment.createPending(diary.getId(), USER_ID, "DIA-" + UUID.randomUUID(), PRICE, NOW);
    payment.markPaid("pk", paidAt);
    return payment;
  }
}
