package com.seoulaim.domain.diary.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.seoulaim.domain.diary.admin.dto.AdminPaymentDetailResponse;
import com.seoulaim.domain.diary.admin.dto.AdminPaymentPageResponse;
import com.seoulaim.domain.diary.admin.dto.AdminPaymentResponse;
import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryPayment;
import com.seoulaim.domain.diary.entity.DiaryPaymentMethod;
import com.seoulaim.domain.diary.repository.DiaryPaymentRepository;
import com.seoulaim.domain.diary.repository.DiaryRepository;
import com.seoulaim.domain.diary.service.AdminPaymentService;
import com.seoulaim.domain.inquiry.entity.Inquiry;
import com.seoulaim.domain.inquiry.repository.InquiryRepository;
import com.seoulaim.domain.order.gateway.PaymentCurrency;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.repository.UserRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** 어드민 결제 목록 필터(상태·검색어·기간)·합계·상세를 실제 DB 로 검증한다. */
@Tag("integration")
@SpringBootTest
@Testcontainers
class AdminPaymentIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @Autowired private AdminPaymentService adminPaymentService;
  @Autowired private UserRepository userRepository;
  @Autowired private DiaryRepository diaryRepository;
  @Autowired private DiaryPaymentRepository diaryPaymentRepository;
  @Autowired private InquiryRepository inquiryRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private User mina;
  private User jin;
  private DiaryPayment minaPaid;
  private DiaryPayment minaDuplicate;
  private DiaryPayment jinRefunded;
  private DiaryPayment jinPending;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("delete from inquiry_answers");
    jdbcTemplate.update("delete from inquiries");
    jdbcTemplate.update("delete from diary_payments");
    jdbcTemplate.update("delete from diaries");
    jdbcTemplate.update("delete from users");
    mina = userRepository.save(User.createWithEmail("mina_choi@example.com", "hash", "Mina"));
    jin = userRepository.save(User.createWithEmail("jin@example.com", "hash", "Jin%Park"));

    Diary minaDiary = diaryRepository.save(paidDiary(mina));
    minaPaid = payment(minaDiary, mina, "DIA-20261101-MINA000001", "2026-11-01T01:00:00Z");
    minaPaid.markPaid("pk_mina", Instant.parse("2026-11-01T01:00:00Z"), "카드");
    // 같은 다이어리 중복 결제
    minaDuplicate = payment(minaDiary, mina, "DIA-20261101-MINA000002", "2026-11-01T02:00:00Z");
    minaDuplicate.markPaid("pk_mina2", Instant.parse("2026-11-01T02:00:00Z"), "간편결제");

    Diary jinDiary = diaryRepository.save(paidDiary(jin));
    jinRefunded = payment(jinDiary, jin, "DIA-20261103-JIN0000001", "2026-11-03T15:30:00Z");
    jinRefunded.markPaid("pk_jin", Instant.parse("2026-11-03T15:30:00Z"));
    jinRefunded.markRefunded("tx_jin", Instant.parse("2026-11-04T00:00:00Z"));
    jinPending = payment(jinDiary, jin, "DIA-20261105-JIN0000002", "2026-11-05T00:00:00Z");
    diaryPaymentRepository.saveAllAndFlush(
        List.of(minaPaid, minaDuplicate, jinRefunded, jinPending));
    // 생성 시각은 감사(auditing)가 now 로 채우므로 필터 검증용으로 직접 맞춘다
    for (DiaryPayment payment : List.of(minaPaid, minaDuplicate, jinRefunded, jinPending)) {
      jdbcTemplate.update(
          "update diary_payments set created_at = ? where id = ?",
          java.sql.Timestamp.from(payment.getRefundConsentAt()),
          payment.getId());
    }
    inquiryRepository.save(
        Inquiry.createRefund(
            jin.getId(), "Refund", "please", List.of(), "en", jinRefunded.getId(), true));
  }

  private static Diary paidDiary(User user) {
    Diary diary = Diary.create(user.getId(), false);
    diary.markPaid();
    return diary;
  }

  // refundConsentAt 을 생성 시각으로 쓴다
  private static DiaryPayment payment(Diary diary, User user, String orderNumber, String at) {
    return DiaryPayment.createPending(
        diary.getId(), user.getId(), orderNumber, 3000, Instant.parse(at));
  }

  private static List<UUID> ids(AdminPaymentPageResponse page) {
    return page.items().stream().map(AdminPaymentResponse::id).toList();
  }

  @Test
  @DisplayName("필터 없으면 전체 최신순, 합계는 paid 2건 6000원·refunded 1건 3000원")
  void getPayments_전체_최신순과합계() {
    AdminPaymentPageResponse page = adminPaymentService.getPayments(null, null, null, null, 0, 50);

    assertThat(ids(page))
        .containsExactly(
            jinPending.getId(), jinRefunded.getId(), minaDuplicate.getId(), minaPaid.getId());
    assertThat(page.totalElements()).isEqualTo(4);
    assertThat(page.summary())
        .isEqualTo(
            new AdminPaymentPageResponse.Summary(
                2,
                6000,
                1,
                3000,
                Map.of(PaymentCurrency.KRW, new BigDecimal("6000")),
                Map.of(PaymentCurrency.KRW, new BigDecimal("3000"))));
    AdminPaymentResponse duplicate = page.items().get(2);
    assertThat(duplicate.orderId()).isEqualTo("DIA-20261101-MINA000002");
    assertThat(duplicate.member().email()).isEqualTo("mina_choi@example.com");
    assertThat(duplicate.status()).isEqualTo("paid");
    assertThat(duplicate.method()).isEqualTo(DiaryPaymentMethod.CARD);
    assertThat(duplicate.methodDetail()).isEqualTo("간편결제");
    assertThat(duplicate.currency()).isEqualTo(PaymentCurrency.KRW);
    assertThat(duplicate.amount()).isEqualTo(new BigDecimal("3000"));
    assertThat(duplicate.sameDiaryPayments()).isEqualTo(1);
    AdminPaymentResponse refunded = page.items().get(1);
    assertThat(refunded.status()).isEqualTo("refunded");
    assertThat(refunded.refundRequestId()).isNotNull();
    assertThat(refunded.refundRequested()).isTrue();
    assertThat(refunded.sameDiaryPayments()).isZero();
  }

  @Test
  @DisplayName("PayPal(USD) 결제는 method·currency·2.99 로 내려가고, 합계는 통화별로 나누며 paidAmount 는 원화만")
  void getPayments_PAYPAL_통화별합계() {
    Diary diary = diaryRepository.save(paidDiary(mina));
    DiaryPayment paypal =
        DiaryPayment.createPending(
            diary.getId(),
            mina.getId(),
            "DIA-20261107-PAYPAL0001",
            DiaryPaymentMethod.PAYPAL,
            new BigDecimal("2.99"),
            Instant.parse("2026-11-07T00:00:00Z"));
    paypal.markPaid("pk_pp", Instant.parse("2026-11-07T00:00:00Z"), "해외간편결제(PAYPAL)");
    diaryPaymentRepository.saveAndFlush(paypal);

    AdminPaymentPageResponse page = adminPaymentService.getPayments(null, null, null, null, 0, 50);

    AdminPaymentResponse item =
        page.items().stream()
            .filter(it -> it.id().equals(paypal.getId()))
            .findFirst()
            .orElseThrow();
    assertThat(item.method()).isEqualTo(DiaryPaymentMethod.PAYPAL);
    assertThat(item.currency()).isEqualTo(PaymentCurrency.USD);
    assertThat(item.amount()).isEqualTo(new BigDecimal("2.99"));
    assertThat(item.methodDetail()).isEqualTo("해외간편결제(PAYPAL)");
    assertThat(page.summary().paidCount()).isEqualTo(3);
    assertThat(page.summary().paidAmount()).isEqualTo(6000);
    assertThat(page.summary().paidAmounts())
        .containsExactly(
            Map.entry(PaymentCurrency.KRW, new BigDecimal("6000")),
            Map.entry(PaymentCurrency.USD, new BigDecimal("2.99")));
  }

  @Test
  @DisplayName("status 필터는 목록만 좁히고 합계는 그대로. 대소문자 무시, 없는 값은 400")
  void getPayments_상태필터() {
    AdminPaymentPageResponse page =
        adminPaymentService.getPayments("REFUNDED", null, null, null, 0, 50);

    assertThat(ids(page)).containsExactly(jinRefunded.getId());
    assertThat(page.totalElements()).isEqualTo(1);
    assertThat(page.summary().paidCount()).isEqualTo(2);
    assertThatThrownBy(() -> adminPaymentService.getPayments("refund", null, null, null, 0, 50))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);
  }

  @Test
  @DisplayName("q 는 주문번호·이메일·닉네임 부분 일치(대소문자 무시)이고 % _ 는 문자 그대로 찾는다")
  void getPayments_검색어() {
    assertThat(ids(adminPaymentService.getPayments(null, "mina000002", null, null, 0, 50)))
        .containsExactly(minaDuplicate.getId());
    assertThat(ids(adminPaymentService.getPayments(null, "MINA_CHOI@", null, null, 0, 50)))
        .containsExactly(minaDuplicate.getId(), minaPaid.getId());
    assertThat(ids(adminPaymentService.getPayments(null, "jin%p", null, null, 0, 50)))
        .containsExactly(jinPending.getId(), jinRefunded.getId());
    // _ 가 와일드카드면 "minaxchoi" 같은 값도 걸리지만 이스케이프해서 정확히 '_' 만
    assertThat(ids(adminPaymentService.getPayments(null, "a_c", null, null, 0, 50)))
        .containsExactly(minaDuplicate.getId(), minaPaid.getId());
    AdminPaymentPageResponse none =
        adminPaymentService.getPayments(null, "nobody", null, null, 0, 50);
    assertThat(none.items()).isEmpty();
    assertThat(none.summary())
        .isEqualTo(new AdminPaymentPageResponse.Summary(0, 0, 0, 0, Map.of(), Map.of()));
  }

  @Test
  @DisplayName("from/to 는 생성일 KST 기준 양끝 포함 (2026-11-03T15:30Z = 11-04 KST)")
  void getPayments_기간() {
    LocalDate nov4 = LocalDate.of(2026, 11, 4);

    AdminPaymentPageResponse page = adminPaymentService.getPayments(null, null, nov4, nov4, 0, 50);

    assertThat(ids(page)).containsExactly(jinRefunded.getId());
    assertThat(page.summary())
        .isEqualTo(
            new AdminPaymentPageResponse.Summary(
                0, 0, 1, 3000, Map.of(), Map.of(PaymentCurrency.KRW, new BigDecimal("3000"))));
    assertThat(
            ids(
                adminPaymentService.getPayments(
                    null, null, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), 0, 50)))
        .containsExactly(minaDuplicate.getId(), minaPaid.getId());
    assertThatThrownBy(
            () -> adminPaymentService.getPayments(null, null, nov4, nov4.minusDays(1), 0, 50))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);
  }

  @Test
  @DisplayName("page/size 로 나누고 totalElements 는 전체 건수")
  void getPayments_페이지() {
    AdminPaymentPageResponse second = adminPaymentService.getPayments(null, null, null, null, 1, 3);

    assertThat(ids(second)).containsExactly(minaPaid.getId());
    assertThat(second.totalElements()).isEqualTo(4);
    assertThat(second.page()).isEqualTo(1);
    assertThat(second.size()).isEqualTo(3);
  }

  @Test
  @DisplayName("상세는 토스 키·환불 요청을 포함하고, 없는 결제는 404 PAYMENT_NOT_FOUND")
  void getPayment_상세() {
    AdminPaymentDetailResponse detail = adminPaymentService.getPayment(jinRefunded.getId());

    assertThat(detail.paymentKey()).isEqualTo("pk_jin");
    assertThat(detail.tossCancelKey()).isEqualTo("tx_jin");
    assertThat(detail.refundRequest().status()).isEqualTo("requested");
    assertThat(detail.payment().refundedAt()).isEqualTo(Instant.parse("2026-11-04T00:00:00Z"));
    assertThat(adminPaymentService.getPayment(jinPending.getId()).refundRequest()).isNull();
    assertThatThrownBy(() -> adminPaymentService.getPayment(UUID.randomUUID()))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
  }
}
