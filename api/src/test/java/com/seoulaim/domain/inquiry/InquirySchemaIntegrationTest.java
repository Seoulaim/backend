package com.seoulaim.domain.inquiry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryPayment;
import com.seoulaim.domain.diary.repository.DiaryPaymentRepository;
import com.seoulaim.domain.diary.repository.DiaryRepository;
import com.seoulaim.domain.inquiry.entity.Inquiry;
import com.seoulaim.domain.inquiry.entity.InquiryAnswer;
import com.seoulaim.domain.inquiry.entity.InquiryCategory;
import com.seoulaim.domain.inquiry.entity.RefundStatus;
import com.seoulaim.domain.inquiry.repository.InquiryAnswerRepository;
import com.seoulaim.domain.inquiry.repository.InquiryRepository;
import com.seoulaim.domain.inquiry.service.RefundRequestQueryService;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@SpringBootTest
@Testcontainers
class InquirySchemaIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @Autowired private UserRepository userRepository;
  @Autowired private DiaryRepository diaryRepository;
  @Autowired private DiaryPaymentRepository diaryPaymentRepository;
  @Autowired private InquiryRepository inquiryRepository;
  @Autowired private InquiryAnswerRepository inquiryAnswerRepository;
  @Autowired private RefundRequestQueryService refundRequestQueryService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("일반 문의·답변이 photo_keys jsonb 와 함께 저장·조회된다")
  void save_일반문의와답변_왕복() {
    UUID userId = saveUser();
    Inquiry inquiry =
        inquiryRepository.saveAndFlush(
            Inquiry.create(
                userId,
                InquiryCategory.DIARY,
                "PDF 문의",
                "내용",
                List.of("inquiry/2026/11/a.jpg", "inquiry/2026/11/b.jpg"),
                "ko"));
    inquiryAnswerRepository.saveAndFlush(
        InquiryAnswer.create(inquiry.getId(), "답변", Instant.parse("2026-11-05T01:00:00Z")));

    Inquiry found = inquiryRepository.findById(inquiry.getId()).orElseThrow();
    assertThat(found.getPhotoKeys())
        .containsExactly("inquiry/2026/11/a.jpg", "inquiry/2026/11/b.jpg");
    assertThat(found.getRefundStatus()).isNull();
    assertThat(
            inquiryAnswerRepository.findAllByInquiryIdInOrderBySentAtAscIdAsc(
                List.of(found.getId())))
        .extracting(InquiryAnswer::getBody)
        .containsExactly("답변");
  }

  @Test
  @DisplayName("같은 결제에 REQUESTED 환불 문의는 하나만 허용되고, 거절된 뒤에는 다시 신청할 수 있다")
  void refund_처리중요청_결제당하나() {
    UUID userId = saveUser();
    DiaryPayment payment = savePaidPayment(userId);
    Inquiry first = inquiryRepository.saveAndFlush(refund(userId, payment.getId()));

    assertThat(refundRequestQueryService.findRequestedPaymentIds(List.of(payment.getId())))
        .containsExactly(payment.getId());
    assertThatThrownBy(() -> inquiryRepository.saveAndFlush(refund(userId, payment.getId())))
        .isInstanceOf(DataIntegrityViolationException.class);

    jdbcTemplate.update(
        "UPDATE inquiries SET refund_status = 'REJECTED' WHERE id = ?", first.getId());
    inquiryRepository.saveAndFlush(refund(userId, payment.getId()));
    assertThat(
            inquiryRepository.existsByPaymentIdAndRefundStatusIn(
                payment.getId(), List.of(RefundStatus.REQUESTED)))
        .isTrue();
  }

  @Test
  @DisplayName("REFUND 가 아닌 문의는 결제·환불 상태를 가질 수 없다 (CHECK)")
  void check_환불필드는REFUND만() {
    UUID userId = saveUser();
    DiaryPayment payment = savePaidPayment(userId);

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO inquiries (id, user_id, category, title, content, language, status,
                      payment_id, refund_within_policy, refund_status, created_at, updated_at)
                    VALUES (?, ?, 'OTHER', 't', 'c', 'en', 'RECEIVED', ?, true, 'REQUESTED',
                      now(), now())
                    """,
                    UUID.randomUUID(),
                    userId,
                    payment.getId()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("diary_payments 에 refunded_at·toss_cancel_key 컬럼이 추가된다")
  void migration_diaryPayments_환불컬럼() {
    Integer columns =
        jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM information_schema.columns
            WHERE table_name = 'diary_payments' AND column_name IN ('refunded_at', 'toss_cancel_key')
            """,
            Integer.class);
    assertThat(columns).isEqualTo(2);
  }

  private static Inquiry refund(UUID userId, UUID paymentId) {
    return Inquiry.createRefund(userId, "환불", "내용", List.of(), "ko", paymentId, true);
  }

  private DiaryPayment savePaidPayment(UUID userId) {
    Diary diary = diaryRepository.save(Diary.create(userId, false));
    DiaryPayment payment =
        DiaryPayment.createPending(
            diary.getId(), userId, "DIA-" + UUID.randomUUID(), 3000L, Instant.now());
    payment.markPaid("pk_" + UUID.randomUUID(), Instant.now());
    return diaryPaymentRepository.saveAndFlush(payment);
  }

  private UUID saveUser() {
    return userRepository
        .save(User.createWithEmail(UUID.randomUUID() + "@test.com", "hash", "tester"))
        .getId();
  }
}
