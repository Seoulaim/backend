package com.seoulaim.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.diary.dto.DiaryPaymentConfirmRequest;
import com.seoulaim.domain.diary.dto.DiaryPaymentConfirmResponse;
import com.seoulaim.domain.diary.dto.DiaryPaymentCreateRequest;
import com.seoulaim.domain.diary.dto.DiaryPaymentCreateResponse;
import com.seoulaim.domain.diary.dto.MyPaymentResponse;
import com.seoulaim.domain.diary.dto.PaymentRefundInfo;
import com.seoulaim.domain.diary.dto.RefundRequestCheck;
import com.seoulaim.domain.diary.dto.UserDiaryStats;
import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryDownload;
import com.seoulaim.domain.diary.entity.DiaryDownloadFormat;
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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DiaryPaymentServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-03T03:00:00Z");
  private static final UUID USER_ID = UUID.randomUUID();
  private static final long PRICE = 3000L;
  private static final String CANCEL_REASON = "고객 요청 환불";

  @Mock private DiaryService diaryService;
  @Mock private DiaryRepository diaryRepository;
  @Mock private DiaryPaymentRepository diaryPaymentRepository;
  @Mock private DiaryDownloadRepository diaryDownloadRepository;
  @Mock private PaymentGateway paymentGateway;
  @Mock private RefundRequestQueryService refundRequestQueryService;

  private DiaryPaymentService diaryPaymentService;

  @BeforeEach
  void setUp() {
    diaryPaymentService =
        new DiaryPaymentService(
            diaryService,
            diaryRepository,
            diaryPaymentRepository,
            diaryDownloadRepository,
            paymentGateway,
            refundRequestQueryService,
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
  @DisplayName("결제 생성 응답에 설정(app.payment.toss.client-key)의 토스 클라이언트 키가 포함된다")
  void createPayment_정상_클라이언트키포함() {
    Diary diary = givenMyDiary(Diary.create(USER_ID, false));
    given(diaryPaymentRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

    DiaryPaymentCreateResponse response =
        diaryPaymentService.createPayment(
            USER_ID, diary.getId(), new DiaryPaymentCreateRequest(null, true));

    assertThat(response.tossClientKey()).isEqualTo("test_ck").isNotBlank();
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
  @DisplayName("승인 성공 시 결제는 PAID(토스 결제수단 저장), 다이어리는 paid=true 가 된다")
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
    assertThat(payment.getMethod()).isEqualTo("카드");
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
    finalized.finalizeWith(List.of(), Map.of(), 0, NOW);
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
    given(refundRequestQueryService.findRequestedPaymentIds(any())).willReturn(Set.of());

    List<MyPaymentResponse> responses = diaryPaymentService.getMyPayments(USER_ID);

    assertThat(responses)
        .extracting(MyPaymentResponse::refundable, MyPaymentResponse::withinPolicy)
        .containsExactly(
            tuple(true, true),
            tuple(true, false),
            tuple(false, false),
            tuple(false, false),
            tuple(true, true));
    assertThat(responses).noneMatch(MyPaymentResponse::refundRequested);
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

  @Test
  @DisplayName("결제 내역: 처리 대기 환불 문의가 있으면 refundRequested=true, refundable·withinPolicy=false")
  void getMyPayments_환불요청중_refundable아님() {
    Diary diary = Diary.create(USER_ID, false);
    DiaryPayment requested = paid(diary, NOW.minus(Duration.ofDays(1)));
    DiaryPayment other = paid(diary, NOW.minus(Duration.ofDays(1)));
    given(
            diaryPaymentRepository.findAllByUserIdAndStatusInOrderByPaidAtDescIdDesc(
                USER_ID, List.of(DiaryPaymentStatus.PAID, DiaryPaymentStatus.REFUNDED)))
        .willReturn(List.of(requested, other));
    given(diaryRepository.findAllByIdIn(any())).willReturn(List.of(diary));
    given(
            refundRequestQueryService.findRequestedPaymentIds(
                List.of(requested.getId(), other.getId())))
        .willReturn(Set.of(requested.getId()));

    List<MyPaymentResponse> responses = diaryPaymentService.getMyPayments(USER_ID);

    assertThat(responses)
        .extracting(
            MyPaymentResponse::refundRequested,
            MyPaymentResponse::refundable,
            MyPaymentResponse::withinPolicy)
        .containsExactly(tuple(true, false, false), tuple(false, true, true));
  }

  @Test
  @DisplayName("환불 정책 판정: 7일 이내·미완성·편집 없음이면 정책 안")
  void checkRefundRequest_정상_정책안() {
    Diary diary = Diary.create(USER_ID, false);
    DiaryPayment payment = givenOwnedPayment(diary, NOW.minus(Duration.ofDays(6)));

    assertThat(diaryPaymentService.checkRefundRequest(USER_ID, payment.getId()))
        .contains(new RefundRequestCheck(payment.getId(), true));
  }

  @Test
  @DisplayName("환불 정책 판정: 결제 후 7일이 지나면 정책 밖 (접수는 가능)")
  void checkRefundRequest_7일초과_정책밖() {
    Diary diary = Diary.create(USER_ID, false);
    DiaryPayment payment =
        givenOwnedPayment(diary, NOW.minus(Duration.ofDays(7)).minus(Duration.ofMinutes(1)));

    assertThat(diaryPaymentService.checkRefundRequest(USER_ID, payment.getId()))
        .contains(new RefundRequestCheck(payment.getId(), false));
  }

  @Test
  @DisplayName("환불 정책 판정: 결제 후 편집 이력이 있으면 정책 밖")
  void checkRefundRequest_편집후_정책밖() {
    Diary diary = Diary.create(USER_ID, false);
    DiaryPayment payment = givenOwnedPayment(diary, NOW.minus(Duration.ofDays(1)));
    payment.recordEdited(NOW.minus(Duration.ofHours(1)));

    assertThat(diaryPaymentService.checkRefundRequest(USER_ID, payment.getId()))
        .contains(new RefundRequestCheck(payment.getId(), false));
  }

  @Test
  @DisplayName("환불 정책 판정: 완성(FINALIZED)한 다이어리면 정책 밖")
  void checkRefundRequest_완성후_정책밖() {
    Diary diary = Diary.create(USER_ID, false);
    diary.finalizeWith(List.of(), Map.of(), 0, NOW);
    DiaryPayment payment = givenOwnedPayment(diary, NOW.minus(Duration.ofDays(1)));

    assertThat(diaryPaymentService.checkRefundRequest(USER_ID, payment.getId()))
        .contains(new RefundRequestCheck(payment.getId(), false));
  }

  @Test
  @DisplayName("환불 정책 판정: PAID 가 아니거나 남의 결제면 empty")
  void checkRefundRequest_PAID아님_empty() {
    Diary diary = Diary.create(USER_ID, false);
    DiaryPayment pending = DiaryPayment.createPending(diary.getId(), USER_ID, "DIA-P", PRICE, NOW);
    given(diaryPaymentRepository.findByIdAndUserId(pending.getId(), USER_ID))
        .willReturn(Optional.of(pending));
    UUID others = UUID.randomUUID();
    given(diaryPaymentRepository.findByIdAndUserId(others, USER_ID)).willReturn(Optional.empty());

    assertThat(diaryPaymentService.checkRefundRequest(USER_ID, pending.getId())).isEmpty();
    assertThat(diaryPaymentService.checkRefundRequest(USER_ID, others)).isEmpty();
  }

  private DiaryPayment givenOwnedPayment(Diary diary, Instant paidAt) {
    DiaryPayment payment = paid(diary, paidAt);
    given(diaryPaymentRepository.findByIdAndUserId(payment.getId(), USER_ID))
        .willReturn(Optional.of(payment));
    given(diaryRepository.findById(diary.getId())).willReturn(Optional.of(diary));
    return payment;
  }

  @Test
  @DisplayName("환불: 첫(무료) 다이어리는 결제만 REFUNDED·paid=false 로 되돌리고 꾸민 내용은 남긴다")
  void refund_첫다이어리_paid만해제() {
    Diary first = Diary.create(USER_ID, true);
    first.saveSpreads(List.of(Map.of("p", 1), Map.of("p", 2), Map.of("p", 3)), Map.of(), 0, NOW);
    first.markPaid();
    DiaryPayment payment = givenLockedPaidPayment(first);
    given(paymentGateway.cancel("pk", CANCEL_REASON))
        .willReturn(PaymentCancelResult.success("tx_1", NOW.minusSeconds(5), "{}"));

    PaymentRefundInfo info = diaryPaymentService.refund(payment.getId(), CANCEL_REASON);

    assertThat(payment.getStatus()).isEqualTo(DiaryPaymentStatus.REFUNDED);
    assertThat(payment.getTossCancelKey()).isEqualTo("tx_1");
    assertThat(payment.getRefundedAt()).isEqualTo(NOW.minusSeconds(5));
    assertThat(first.isPaid()).isFalse();
    assertThat(first.isDeleted()).isFalse();
    assertThat(first.getSpreads()).hasSize(3);
    assertThat(info.diary().first()).isTrue();
    assertThat(info.diary().pages()).isEqualTo(3);
  }

  @Test
  @DisplayName("환불: 두 번째 이후 다이어리는 soft delete 한다")
  void refund_두번째다이어리_softDelete() {
    Diary second = Diary.create(USER_ID, false);
    second.markPaid();
    DiaryPayment payment = givenLockedPaidPayment(second);
    given(paymentGateway.cancel("pk", CANCEL_REASON))
        .willReturn(PaymentCancelResult.success("tx_2", null, "{}"));

    diaryPaymentService.refund(payment.getId(), CANCEL_REASON);

    assertThat(payment.getStatus()).isEqualTo(DiaryPaymentStatus.REFUNDED);
    assertThat(payment.getRefundedAt()).isEqualTo(NOW);
    assertThat(second.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("환불: 토스가 취소를 거절하면 502 PAYMENT_REFUND_FAILED, 결제·다이어리 상태는 그대로")
  void refund_토스실패_상태불변() {
    Diary second = Diary.create(USER_ID, false);
    second.markPaid();
    DiaryPayment payment = givenLockedPaidPayment(second);
    given(paymentGateway.cancel("pk", CANCEL_REASON))
        .willReturn(PaymentCancelResult.failure("NOT_CANCELABLE_PAYMENT", "취소 불가", "{}"));

    assertThatThrownBy(() -> diaryPaymentService.refund(payment.getId(), CANCEL_REASON))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_REFUND_FAILED);
              assertThat(exception.getFields())
                  .containsEntry("code", "NOT_CANCELABLE_PAYMENT")
                  .containsEntry("message", "취소 불가");
            });
    assertThat(payment.getStatus()).isEqualTo(DiaryPaymentStatus.PAID);
    assertThat(payment.getRefundedAt()).isNull();
    assertThat(second.isPaid()).isTrue();
    assertThat(second.isDeleted()).isFalse();
  }

  @Test
  @DisplayName("환불: 토스에서 이미 취소된 결제(ALREADY_CANCELED_PAYMENT)는 환불 완료로 기록한다")
  void refund_이미취소됨_환불완료() {
    Diary second = Diary.create(USER_ID, false);
    DiaryPayment payment = givenLockedPaidPayment(second);
    given(paymentGateway.cancel("pk", CANCEL_REASON))
        .willReturn(PaymentCancelResult.failure("ALREADY_CANCELED_PAYMENT", "이미 취소", "{}"));

    diaryPaymentService.refund(payment.getId(), CANCEL_REASON);

    assertThat(payment.getStatus()).isEqualTo(DiaryPaymentStatus.REFUNDED);
    assertThat(payment.getTossCancelKey()).isNull();
  }

  @Test
  @DisplayName("환불: PAID 가 아닌 결제는 토스를 부르지 않고 PAYMENT_REFUND_FAILED")
  void refund_PAID아님_게이트웨이호출안함() {
    Diary diary = Diary.create(USER_ID, false);
    DiaryPayment pending = DiaryPayment.createPending(diary.getId(), USER_ID, "DIA-P", PRICE, NOW);
    given(diaryPaymentRepository.findWithLockById(pending.getId()))
        .willReturn(Optional.of(pending));

    assertThatThrownBy(() -> diaryPaymentService.refund(pending.getId(), CANCEL_REASON))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PAYMENT_REFUND_FAILED);
    verify(paymentGateway, never()).cancel(anyString(), anyString());
  }

  @Test
  @DisplayName("환불 관리 정보: 다운로드 기록이 있으면 policy.notDownloaded=false, 다이어리에 다운로드 이력이 붙는다")
  void findRefundInfos_다운로드기록후_notDownloadedFalse() {
    Diary diary = Diary.create(USER_ID, false);
    diary.markPaid();
    diary.finalizeWith(List.of(), Map.of(), 2, NOW.minusSeconds(3600));
    DiaryPayment payment = paid(diary, NOW.minus(Duration.ofDays(1)));
    DiaryDownload download =
        DiaryDownload.create(diary.getId(), USER_ID, DiaryDownloadFormat.JPG, 5);
    ReflectionTestUtils.setField(download, "createdAt", NOW.minusSeconds(60));
    given(diaryPaymentRepository.findAllById(List.of(payment.getId())))
        .willReturn(List.of(payment));
    given(diaryRepository.findAllByIdIn(List.of(diary.getId()))).willReturn(List.of(diary));
    given(
            diaryDownloadRepository.findAllByDiaryIdInOrderByCreatedAtDescIdDesc(
                List.of(diary.getId())))
        .willReturn(List.of(download));

    PaymentRefundInfo info =
        diaryPaymentService.findRefundInfos(List.of(payment.getId())).getFirst();

    assertThat(info.policy().notDownloaded().ok()).isFalse();
    assertThat(info.policy().notDownloaded().downloadCount()).isEqualTo(1);
    assertThat(info.policy().notDownloaded().lastDownloadedAt()).isEqualTo(NOW.minusSeconds(60));
    assertThat(info.policy().within7Days().ok()).isTrue();
    assertThat(info.policy().allOk()).isFalse();
    assertThat(info.diary().downloads())
        .containsExactly(
            new PaymentRefundInfo.DownloadInfo(DiaryDownloadFormat.JPG, 5, NOW.minusSeconds(60)));
    assertThat(info.diary().photoCount()).isEqualTo(2);
    assertThat(info.diary().pageKeys()).isEmpty();
  }

  @Test
  @DisplayName("회원별 통계: 보이는 다이어리 수와 PAID 결제 수, 없는 회원은 0")
  void findUserStats_집계() {
    UUID other = UUID.randomUUID();
    UserCount diaries = userCount(USER_ID, 2);
    UserCount payments = userCount(USER_ID, 1);
    given(diaryRepository.countVisibleByUserIds(List.of(USER_ID, other)))
        .willReturn(List.of(diaries));
    given(
            diaryPaymentRepository.countByUserIdsAndStatus(
                List.of(USER_ID, other), DiaryPaymentStatus.PAID))
        .willReturn(List.of(payments));

    Map<UUID, UserDiaryStats> stats = diaryPaymentService.findUserStats(List.of(USER_ID, other));

    assertThat(stats.get(USER_ID)).isEqualTo(new UserDiaryStats(2, 1));
    assertThat(stats.get(other)).isEqualTo(UserDiaryStats.EMPTY);
  }

  private static UserCount userCount(UUID userId, long count) {
    return new UserCount() {
      @Override
      public UUID getUserId() {
        return userId;
      }

      @Override
      public long getCount() {
        return count;
      }
    };
  }

  private DiaryPayment givenLockedPaidPayment(Diary diary) {
    DiaryPayment payment = paid(diary, NOW.minus(Duration.ofDays(1)));
    given(diaryPaymentRepository.findWithLockById(payment.getId()))
        .willReturn(Optional.of(payment));
    lenient().when(diaryRepository.findById(diary.getId())).thenReturn(Optional.of(diary));
    return payment;
  }
}
