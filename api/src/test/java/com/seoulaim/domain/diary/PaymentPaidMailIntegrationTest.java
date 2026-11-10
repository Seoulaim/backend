package com.seoulaim.domain.diary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.diary.dto.DiaryPaymentConfirmRequest;
import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryPayment;
import com.seoulaim.domain.diary.entity.DiaryPaymentStatus;
import com.seoulaim.domain.diary.repository.DiaryPaymentRepository;
import com.seoulaim.domain.diary.repository.DiaryRepository;
import com.seoulaim.domain.diary.service.DiaryPaymentService;
import com.seoulaim.domain.order.gateway.PaymentGateway;
import com.seoulaim.domain.order.gateway.PaymentResult;
import com.seoulaim.domain.user.entity.User;
import com.seoulaim.domain.user.repository.UserRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.mail.EmailSender;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** 실제 트랜잭션에서 결제 승인 커밋 후 결제 완료 메일 발송을 확인한다. 토스·메일은 mock. */
@Tag("integration")
@SpringBootTest
@Testcontainers
class PaymentPaidMailIntegrationTest {

  private static final BigDecimal AMOUNT = new BigDecimal("3000");

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  // 원화 MID 게이트웨이 (CARD)
  @MockitoBean private PaymentGateway paymentGateway;
  @MockitoBean private EmailSender emailSender;

  @Autowired private UserRepository userRepository;
  @Autowired private DiaryRepository diaryRepository;
  @Autowired private DiaryPaymentRepository diaryPaymentRepository;
  @Autowired private DiaryPaymentService diaryPaymentService;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("승인 성공: 커밋된 뒤(트랜잭션 밖) 회원 언어로 메일 1번, 중복 승인에는 다시 보내지 않는다")
  void confirm_성공_커밋후메일_중복없음() {
    Fixture fixture = fixture("ja");
    given(paymentGateway.confirm(eq("pk_1"), eq(fixture.orderNumber()), any(BigDecimal.class)))
        .willReturn(PaymentResult.success("pk_1", "카드", Instant.now(), "{}"));
    // 메일 발송 시점에 다른 커넥션(별도 스레드)에서 PAID 가 보이면 커밋 후 발송이다
    AtomicReference<String> statusSeenBySender = new AtomicReference<>();
    doAnswer(
            invocation -> {
              statusSeenBySender.set(
                  CompletableFuture.supplyAsync(
                          () ->
                              jdbcTemplate.queryForObject(
                                  "select status from diary_payments where id = ?",
                                  String.class,
                                  fixture.paymentId()))
                      .join());
              return null;
            })
        .when(emailSender)
        .send(anyString(), anyString(), anyString(), anyString());

    diaryPaymentService.confirm(
        fixture.userId(), fixture.paymentId(), new DiaryPaymentConfirmRequest("pk_1", AMOUNT));
    diaryPaymentService.confirm(
        fixture.userId(), fixture.paymentId(), new DiaryPaymentConfirmRequest("pk_1", AMOUNT));

    verify(emailSender, times(1))
        .send(
            eq(fixture.email()),
            eq("[Seoulaim] お支払い完了 – My Diary"),
            contains("KRW 3,000"),
            contains(fixture.orderNumber()));
    assertThat(statusSeenBySender).hasValue("PAID");
  }

  @Test
  @DisplayName("메일 발송이 실패해도 승인은 커밋되고 응답은 정상이다")
  void confirm_메일실패_승인유지() {
    Fixture fixture = fixture("en");
    given(paymentGateway.confirm(eq("pk_2"), eq(fixture.orderNumber()), any(BigDecimal.class)))
        .willReturn(PaymentResult.success("pk_2", "카드", Instant.now(), "{}"));
    willThrow(new BusinessException(ErrorCode.AUTH_MAIL_SEND_FAILED))
        .given(emailSender)
        .send(anyString(), anyString(), anyString(), anyString());

    assertThat(
            diaryPaymentService
                .confirm(
                    fixture.userId(),
                    fixture.paymentId(),
                    new DiaryPaymentConfirmRequest("pk_2", AMOUNT))
                .diaryId())
        .isEqualTo(fixture.diaryId());

    assertThat(diaryPaymentRepository.findById(fixture.paymentId()).orElseThrow().getStatus())
        .isEqualTo(DiaryPaymentStatus.PAID);
    assertThat(diaryRepository.findById(fixture.diaryId()).orElseThrow().isPaid()).isTrue();
  }

  @Test
  @DisplayName("토스 승인 실패: 결제 FAILED 는 저장되고 메일은 없다")
  void confirm_토스실패_메일없음() {
    Fixture fixture = fixture("en");
    given(paymentGateway.confirm(eq("pk_3"), eq(fixture.orderNumber()), any(BigDecimal.class)))
        .willReturn(PaymentResult.failure("REJECT_CARD_COMPANY", "거절", "{}"));

    assertThatThrownBy(
            () ->
                diaryPaymentService.confirm(
                    fixture.userId(),
                    fixture.paymentId(),
                    new DiaryPaymentConfirmRequest("pk_3", AMOUNT)))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PAYMENT_FAILED);

    assertThat(diaryPaymentRepository.findById(fixture.paymentId()).orElseThrow().getStatus())
        .isEqualTo(DiaryPaymentStatus.FAILED);
    verify(emailSender, times(0)).send(anyString(), anyString(), anyString(), anyString());
  }

  private Fixture fixture(String locale) {
    String email = UUID.randomUUID() + "@test.com";
    User user = User.createWithEmail(email, "hash", "tester");
    user.changeLocale(locale);
    UUID userId = userRepository.save(user).getId();
    Diary diary = diaryRepository.save(Diary.create(userId, false));
    String orderNumber = "DIA-" + UUID.randomUUID().toString().substring(0, 20);
    DiaryPayment payment =
        diaryPaymentRepository.save(
            DiaryPayment.createPending(diary.getId(), userId, orderNumber, 3000L, Instant.now()));
    return new Fixture(email, userId, diary.getId(), payment.getId(), orderNumber);
  }

  private record Fixture(
      String email, UUID userId, UUID diaryId, UUID paymentId, String orderNumber) {}
}
