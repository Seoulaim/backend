package com.seoulaim.domain.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.diary.dto.PaymentRefundInfo;
import com.seoulaim.domain.diary.entity.DiaryPaymentStatus;
import com.seoulaim.domain.diary.service.DiaryPaymentService;
import com.seoulaim.domain.inquiry.admin.dto.AdminRefundApproveRequest;
import com.seoulaim.domain.inquiry.admin.dto.AdminRefundApproveRequest.PolicyException;
import com.seoulaim.domain.inquiry.admin.dto.AdminRefundRejectRequest;
import com.seoulaim.domain.inquiry.admin.dto.AdminRefundRequestResponse;
import com.seoulaim.domain.inquiry.admin.dto.RefundAuthor;
import com.seoulaim.domain.inquiry.entity.Inquiry;
import com.seoulaim.domain.inquiry.entity.InquiryCategory;
import com.seoulaim.domain.inquiry.entity.InquiryStatus;
import com.seoulaim.domain.inquiry.entity.RefundStatus;
import com.seoulaim.domain.inquiry.repository.InquiryRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.mail.EmailSender;
import java.time.Clock;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RefundRequestServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-05T03:00:00Z");
  private static final UUID USER_ID = UUID.randomUUID();
  private static final String EMAIL = "jin@example.com";

  @Mock private InquiryRepository inquiryRepository;
  @Mock private DiaryPaymentService diaryPaymentService;
  @Mock private RefundAuthorLoader refundAuthorLoader;
  @Mock private EmailSender emailSender;
  @Mock private ApplicationEventPublisher eventPublisher;

  private RefundRequestService refundRequestService;

  @BeforeEach
  void setUp() {
    refundRequestService =
        new RefundRequestService(
            inquiryRepository,
            diaryPaymentService,
            refundAuthorLoader,
            emailSender,
            eventPublisher,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static Inquiry refund(boolean withinPolicy) {
    return Inquiry.createRefund(
        USER_ID, "Refund please", "Mistake", List.of(), "en", UUID.randomUUID(), withinPolicy);
  }

  private Inquiry givenLocked(boolean withinPolicy) {
    Inquiry inquiry = refund(withinPolicy);
    given(inquiryRepository.findWithLockByIdAndCategory(inquiry.getId(), InquiryCategory.REFUND))
        .willReturn(Optional.of(inquiry));
    return inquiry;
  }

  private void givenAuthor(String email) {
    given(refundAuthorLoader.load(any(Inquiry.class)))
        .willReturn(new RefundAuthor(USER_ID, "Jin", email, NOW, "en", 1, 1, 0, 0, 1, false));
  }

  private static PaymentRefundInfo refundedPayment(UUID paymentId, boolean firstDiary) {
    return new PaymentRefundInfo(
        paymentId,
        "DIA-20261104-ABCDE12345",
        3000L,
        DiaryPaymentStatus.REFUNDED,
        NOW.minusSeconds(86_400),
        NOW.minusSeconds(86_400),
        null,
        NOW,
        new PaymentRefundInfo.DiaryInfo(
            UUID.randomUUID(),
            "My Diary",
            firstDiary,
            3,
            null,
            !firstDiary,
            NOW.minusSeconds(172_800),
            2,
            NOW.minusSeconds(100_000),
            0,
            List.of(),
            null,
            List.of()),
        null);
  }

  @Test
  @DisplayName("정책 밖 요청을 예외 사유 없이 승인하면 400 INQUIRY_POLICY_EXCEPTION_REQUIRED, 토스를 부르지 않는다")
  void approve_정책밖_예외사유필수() {
    Inquiry inquiry = givenLocked(false);

    assertThatThrownBy(
            () ->
                refundRequestService.approve(inquiry.getId(), new AdminRefundApproveRequest(null)))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INQUIRY_POLICY_EXCEPTION_REQUIRED);
    assertThatThrownBy(
            () ->
                refundRequestService.approve(
                    inquiry.getId(), new AdminRefundApproveRequest(new PolicyException(" ", "m"))))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INQUIRY_POLICY_EXCEPTION_REQUIRED);
    verify(diaryPaymentService, never()).refund(any(), anyString());
    assertThat(inquiry.getRefundStatus()).isEqualTo(RefundStatus.REQUESTED);
  }

  @Test
  @DisplayName("정책 밖 요청도 예외 사유가 있으면 승인되고 사유가 남는다. 기타(other)는 메모가 필요하다")
  void approve_정책밖_예외사유로승인() {
    Inquiry inquiry = givenLocked(false);
    given(diaryPaymentService.refund(inquiry.getPaymentId(), "고객 요청 환불"))
        .willReturn(refundedPayment(inquiry.getPaymentId(), false));
    givenAuthor(EMAIL);

    assertThatThrownBy(
            () ->
                refundRequestService.approve(
                    inquiry.getId(),
                    new AdminRefundApproveRequest(new PolicyException("other", null))))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INQUIRY_INVALID);

    AdminRefundRequestResponse response =
        refundRequestService.approve(
            inquiry.getId(),
            new AdminRefundApproveRequest(new PolicyException("duplicate", "토스 중복 결제 확인")));

    assertThat(inquiry.getRefundExceptionReason()).isEqualTo("duplicate");
    assertThat(inquiry.getRefundExceptionMemo()).isEqualTo("토스 중복 결제 확인");
    assertThat(response.refund().exception().reason()).isEqualTo("duplicate");
    assertThat(response.refund().adminName()).isEqualTo("운영자");
  }

  @Test
  @DisplayName("승인하면 결제를 취소하고 문의를 REFUNDED·ANSWERED 로 바꾼 뒤 커밋 후 메일 이벤트를 발행한다")
  void approve_정상_환불완료() {
    Inquiry inquiry = givenLocked(true);
    given(diaryPaymentService.refund(inquiry.getPaymentId(), "고객 요청 환불"))
        .willReturn(refundedPayment(inquiry.getPaymentId(), true));
    givenAuthor(EMAIL);

    AdminRefundRequestResponse response =
        refundRequestService.approve(inquiry.getId(), new AdminRefundApproveRequest(null));

    assertThat(inquiry.getRefundStatus()).isEqualTo(RefundStatus.REFUNDED);
    assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
    assertThat(inquiry.getRefundDecidedAt()).isEqualTo(NOW);
    assertThat(response.refund().status()).isEqualTo(RefundStatus.REFUNDED);
    assertThat(response.payment().provider()).isEqualTo("toss");
    assertThat(response.payment().diary().first()).isTrue();
    ArgumentCaptor<RefundApprovedEvent> captor = ArgumentCaptor.forClass(RefundApprovedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue().email()).isEqualTo(EMAIL);
    assertThat(captor.getValue().subject()).isEqualTo("[Seoulaim] Re: Refund please");
    assertThat(captor.getValue().text())
        .contains("DIA-20261104-ABCDE12345", "KRW 3,000", "pages 3 and later are locked");
    // 메일은 커밋 후 리스너가 보낸다
    verify(emailSender, never()).send(anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("토스 취소가 실패하면 문의는 REQUESTED 그대로이고 메일도 없다")
  void approve_토스실패_상태불변() {
    Inquiry inquiry = givenLocked(true);
    given(diaryPaymentService.refund(inquiry.getPaymentId(), "고객 요청 환불"))
        .willThrow(
            new BusinessException(
                ErrorCode.PAYMENT_REFUND_FAILED, Map.of("code", "NOT_CANCELABLE_PAYMENT")));

    assertThatThrownBy(() -> refundRequestService.approve(inquiry.getId(), null))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PAYMENT_REFUND_FAILED);
    assertThat(inquiry.getRefundStatus()).isEqualTo(RefundStatus.REQUESTED);
    assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.RECEIVED);
    assertThat(inquiry.getRefundDecidedAt()).isNull();
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("이미 결정된 요청은 승인·거절 모두 409 INQUIRY_ALREADY_DECIDED")
  void approveReject_이미결정_409() {
    Inquiry inquiry = givenLocked(true);
    inquiry.rejectRefund("완성됨", NOW);

    assertThatThrownBy(() -> refundRequestService.approve(inquiry.getId(), null))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INQUIRY_ALREADY_DECIDED);
    assertThatThrownBy(
            () -> refundRequestService.reject(inquiry.getId(), new AdminRefundRejectRequest("x")))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INQUIRY_ALREADY_DECIDED);
    verify(diaryPaymentService, never()).refund(any(), anyString());
  }

  @Test
  @DisplayName("거절하면 REJECTED·ANSWERED 로 바꾸고 문의 언어로 사유 메일을 보낸다")
  void reject_사유메일() {
    Inquiry inquiry = givenLocked(false);
    givenAuthor(EMAIL);
    given(diaryPaymentService.findRefundInfos(List.of(inquiry.getPaymentId())))
        .willReturn(List.of());

    AdminRefundRequestResponse response =
        refundRequestService.reject(
            inquiry.getId(), new AdminRefundRejectRequest(" Finished diaries can't be refunded. "));

    assertThat(inquiry.getRefundStatus()).isEqualTo(RefundStatus.REJECTED);
    assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
    assertThat(response.refund().rejectReason()).isEqualTo("Finished diaries can't be refunded.");
    ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
    verify(emailSender).send(any(), any(), text.capture());
    assertThat(text.getValue()).contains("Reason: Finished diaries can't be refunded.");
  }

  @Test
  @DisplayName("목록은 처리 대기를 오래된 순으로 먼저, 결정된 것을 최신순으로 뒤에 둔다")
  void getRefundRequests_정렬() {
    Inquiry waitingOld = refund(true);
    Inquiry waitingNew = refund(true);
    Inquiry decidedOld = refund(true);
    Inquiry decidedNew = refund(true);
    decidedOld.rejectRefund("x", NOW);
    decidedNew.rejectRefund("y", NOW);
    ReflectionTestUtils.setField(waitingOld, "createdAt", NOW.minusSeconds(400));
    ReflectionTestUtils.setField(decidedOld, "createdAt", NOW.minusSeconds(300));
    ReflectionTestUtils.setField(waitingNew, "createdAt", NOW.minusSeconds(200));
    ReflectionTestUtils.setField(decidedNew, "createdAt", NOW.minusSeconds(100));
    List<Inquiry> ascending = List.of(waitingOld, decidedOld, waitingNew, decidedNew);
    given(inquiryRepository.findAllByCategoryOrderByCreatedAtAscIdAsc(InquiryCategory.REFUND))
        .willReturn(ascending);
    given(refundAuthorLoader.load(ascending)).willReturn(Map.of());
    given(diaryPaymentService.findRefundInfos(any())).willReturn(List.of());

    assertThat(refundRequestService.getRefundRequests())
        .extracting(AdminRefundRequestResponse::id)
        .containsExactly(
            waitingOld.getId(), waitingNew.getId(), decidedNew.getId(), decidedOld.getId());
  }
}
