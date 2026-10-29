package com.seoulaim.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.order.dto.PaymentConfirmRequest;
import com.seoulaim.domain.order.dto.PaymentConfirmResponse;
import com.seoulaim.domain.order.entity.ContactType;
import com.seoulaim.domain.order.entity.Order;
import com.seoulaim.domain.order.entity.OrderStatus;
import com.seoulaim.domain.order.entity.Payment;
import com.seoulaim.domain.order.entity.PaymentStatus;
import com.seoulaim.domain.order.entity.PgProvider;
import com.seoulaim.domain.order.gateway.PaymentGateway;
import com.seoulaim.domain.order.gateway.PaymentResult;
import com.seoulaim.domain.order.repository.OrderRepository;
import com.seoulaim.domain.order.repository.PaymentRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final String PAYMENT_KEY = "tgen_20261029_abc";

  @Mock private OrderRepository orderRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private PaymentGateway paymentGateway;
  @InjectMocks private PaymentService paymentService;

  private Order order;
  private Payment payment;

  @BeforeEach
  void setUp() {
    order =
        Order.createPending(
            "ORD-20261029-AB12CD",
            USER_ID,
            25000L,
            3000L,
            "Kim",
            "서울 중구 1",
            null,
            null,
            ContactType.NONE,
            null,
            Instant.parse("2026-10-29T03:00:00Z"));
    payment =
        Payment.createReady(
            order.getId(), PgProvider.TOSS, order.getOrderNumber(), order.getFinalAmount());
  }

  private void givenOrder() {
    given(orderRepository.findWithLockByIdAndUserId(order.getId(), USER_ID))
        .willReturn(Optional.of(order));
  }

  @Test
  @DisplayName("게이트웨이 승인에 성공하면 결제는 DONE, 주문은 PAID 가 된다")
  void confirm_승인성공_PAID전환() {
    givenOrder();
    given(paymentRepository.findByOrderId(order.getId())).willReturn(Optional.of(payment));
    Instant approvedAt = Instant.parse("2026-10-29T03:01:00Z");
    given(paymentGateway.confirm(PAYMENT_KEY, "ORD-20261029-AB12CD", 28000L))
        .willReturn(PaymentResult.success(PAYMENT_KEY, "카드", approvedAt, "{\"status\":\"DONE\"}"));

    PaymentConfirmResponse response =
        paymentService.confirm(
            USER_ID, order.getId(), new PaymentConfirmRequest(PAYMENT_KEY, 28000L));

    assertThat(response.status()).isEqualTo(OrderStatus.PAID);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DONE);
    assertThat(payment.getPaymentKey()).isEqualTo(PAYMENT_KEY);
    assertThat(payment.getMethod()).isEqualTo("카드");
    assertThat(payment.getApprovedAt()).isEqualTo(approvedAt);
    assertThat(payment.getRawResponse()).isEqualTo("{\"status\":\"DONE\"}");
  }

  @Test
  @DisplayName("게이트웨이 승인에 실패하면 결제·주문을 FAILED 로 바꾸고 토스 code/message 를 담아 PAYMENT_FAILED 를 던진다")
  void confirm_승인실패_FAILED전환후예외() {
    givenOrder();
    given(paymentRepository.findByOrderId(order.getId())).willReturn(Optional.of(payment));
    String raw = "{\"code\":\"REJECT_CARD_COMPANY\",\"message\":\"카드사 거절\"}";
    given(paymentGateway.confirm(PAYMENT_KEY, "ORD-20261029-AB12CD", 28000L))
        .willReturn(PaymentResult.failure("REJECT_CARD_COMPANY", "카드사 거절", raw));

    assertThatThrownBy(
            () ->
                paymentService.confirm(
                    USER_ID, order.getId(), new PaymentConfirmRequest(PAYMENT_KEY, 28000L)))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            exception -> {
              BusinessException business = (BusinessException) exception;
              assertThat(business.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_FAILED);
              assertThat(business.getFields())
                  .isEqualTo(Map.of("code", "REJECT_CARD_COMPANY", "message", "카드사 거절"));
            });
    assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    assertThat(payment.getFailureCode()).isEqualTo("REJECT_CARD_COMPANY");
    assertThat(payment.getFailureMessage()).isEqualTo("카드사 거절");
    assertThat(payment.getRawResponse()).isEqualTo(raw);
  }

  @Test
  @DisplayName("요청 금액이 주문 금액과 다르면 게이트웨이를 호출하지 않고 PAYMENT_AMOUNT_MISMATCH 를 던진다")
  void confirm_금액불일치_게이트웨이미호출() {
    givenOrder();

    assertThatThrownBy(
            () ->
                paymentService.confirm(
                    USER_ID, order.getId(), new PaymentConfirmRequest(PAYMENT_KEY, 100L)))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    verify(paymentGateway, never()).confirm(anyString(), anyString(), anyLong());
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
  }

  @Test
  @DisplayName("이미 PAID 인 주문을 다시 승인하면 게이트웨이 호출 없이 PAID 를 반환한다")
  void confirm_이미결제됨_멱등응답() {
    order.markPaid();
    givenOrder();

    PaymentConfirmResponse response =
        paymentService.confirm(
            USER_ID, order.getId(), new PaymentConfirmRequest(PAYMENT_KEY, 28000L));

    assertThat(response.orderId()).isEqualTo(order.getId());
    assertThat(response.status()).isEqualTo(OrderStatus.PAID);
    verify(paymentGateway, never()).confirm(anyString(), anyString(), anyLong());
  }

  @Test
  @DisplayName("타인 주문이면 ORDER_NOT_FOUND 를 던진다")
  void confirm_타인주문_예외발생() {
    UUID otherUserId = UUID.randomUUID();
    given(orderRepository.findWithLockByIdAndUserId(order.getId(), otherUserId))
        .willReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                paymentService.confirm(
                    otherUserId, order.getId(), new PaymentConfirmRequest(PAYMENT_KEY, 28000L)))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    verify(paymentGateway, never()).confirm(anyString(), anyString(), anyLong());
  }
}
