package com.seoulaim.domain.inquiry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryPayment;
import com.seoulaim.domain.diary.entity.DiaryPaymentStatus;
import com.seoulaim.domain.diary.repository.DiaryPaymentRepository;
import com.seoulaim.domain.diary.repository.DiaryRepository;
import com.seoulaim.domain.inquiry.admin.dto.AdminRefundApproveRequest;
import com.seoulaim.domain.inquiry.admin.dto.AdminRefundRequestResponse;
import com.seoulaim.domain.inquiry.entity.Inquiry;
import com.seoulaim.domain.inquiry.entity.RefundStatus;
import com.seoulaim.domain.inquiry.repository.InquiryRepository;
import com.seoulaim.domain.inquiry.service.RefundRequestService;
import com.seoulaim.domain.order.gateway.PaymentCancelResult;
import com.seoulaim.domain.order.gateway.PaymentGateway;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.repository.UserRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.mail.EmailSender;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** 실제 트랜잭션에서 토스 취소 실패 시 롤백, 성공 시 커밋 후 메일 발송을 확인한다. 토스·메일은 mock. */
@Tag("integration")
@SpringBootTest
@Testcontainers
class RefundApprovalIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @MockitoBean private PaymentGateway paymentGateway;
  @MockitoBean private EmailSender emailSender;

  @Autowired private UserRepository userRepository;
  @Autowired private DiaryRepository diaryRepository;
  @Autowired private DiaryPaymentRepository diaryPaymentRepository;
  @Autowired private InquiryRepository inquiryRepository;
  @Autowired private RefundRequestService refundRequestService;

  @Test
  @DisplayName("승인 성공: 두 번째 다이어리는 삭제, 결제 REFUNDED, 문의 REFUNDED, 커밋 후 환불 완료 메일")
  void approve_성공_커밋후메일() {
    Fixture fixture = fixture(false);
    given(paymentGateway.cancel(eq(fixture.paymentKey()), anyString()))
        .willReturn(PaymentCancelResult.success("tx_1", Instant.now(), "{}"));

    AdminRefundRequestResponse response =
        refundRequestService.approve(fixture.inquiryId(), new AdminRefundApproveRequest(null));

    // 응답의 계정 이력은 승인 결과를 반영한다 (승인된 환불 1, PAID 결제 0, 답변 전 문의 0)
    assertThat(response.author().refundedCount()).isEqualTo(1);
    assertThat(response.author().paidPaymentCount()).isZero();
    assertThat(response.author().pendingInquiryCount()).isZero();
    assertThat(response.author().joinedAt()).isNotNull();
    assertThat(response.payment().diary().deleted()).isTrue();
    assertThat(response.policy().notDownloaded().ok()).isTrue();

    DiaryPayment payment = diaryPaymentRepository.findById(fixture.paymentId()).orElseThrow();
    assertThat(payment.getStatus()).isEqualTo(DiaryPaymentStatus.REFUNDED);
    assertThat(payment.getTossCancelKey()).isEqualTo("tx_1");
    assertThat(diaryRepository.findById(fixture.diaryId()).orElseThrow().isDeleted()).isTrue();
    Inquiry inquiry = inquiryRepository.findById(fixture.inquiryId()).orElseThrow();
    assertThat(inquiry.getRefundStatus()).isEqualTo(RefundStatus.REFUNDED);
    verify(emailSender).send(eq(fixture.email()), eq("[Seoulaim] Re: 환불"), anyString());
  }

  @Test
  @DisplayName("토스 취소 실패: 결제·다이어리·문의 모두 롤백되고 메일도 없다")
  void approve_토스실패_롤백() {
    Fixture fixture = fixture(true);
    given(paymentGateway.cancel(eq(fixture.paymentKey()), anyString()))
        .willReturn(PaymentCancelResult.failure("NOT_CANCELABLE_PAYMENT", "취소 불가", "{}"));

    assertThatThrownBy(() -> refundRequestService.approve(fixture.inquiryId(), null))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PAYMENT_REFUND_FAILED);

    assertThat(diaryPaymentRepository.findById(fixture.paymentId()).orElseThrow().getStatus())
        .isEqualTo(DiaryPaymentStatus.PAID);
    Diary diary = diaryRepository.findById(fixture.diaryId()).orElseThrow();
    assertThat(diary.isPaid()).isTrue();
    assertThat(diary.isDeleted()).isFalse();
    assertThat(inquiryRepository.findById(fixture.inquiryId()).orElseThrow().getRefundStatus())
        .isEqualTo(RefundStatus.REQUESTED);
    verify(emailSender, never()).send(eq(fixture.email()), anyString(), anyString());
  }

  private Fixture fixture(boolean free) {
    String email = UUID.randomUUID() + "@test.com";
    UUID userId = userRepository.save(User.createWithEmail(email, "hash", "tester")).getId();
    Diary diary = Diary.create(userId, free);
    diary.markPaid();
    diaryRepository.save(diary);
    String paymentKey = "pk_" + UUID.randomUUID();
    DiaryPayment payment =
        DiaryPayment.createPending(
            diary.getId(), userId, "DIA-" + UUID.randomUUID(), 3000L, Instant.now());
    payment.markPaid(paymentKey, Instant.now());
    diaryPaymentRepository.save(payment);
    Inquiry inquiry =
        inquiryRepository.save(
            Inquiry.createRefund(userId, "환불", "내용", List.of(), "ko", payment.getId(), true));
    return new Fixture(email, diary.getId(), payment.getId(), paymentKey, inquiry.getId());
  }

  private record Fixture(
      String email, UUID diaryId, UUID paymentId, String paymentKey, UUID inquiryId) {}
}
