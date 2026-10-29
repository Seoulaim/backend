package com.seoulaim.domain.order.service;

import com.seoulaim.domain.order.dto.PaymentConfirmRequest;
import com.seoulaim.domain.order.dto.PaymentConfirmResponse;
import com.seoulaim.domain.order.entity.Order;
import com.seoulaim.domain.order.entity.Payment;
import com.seoulaim.domain.order.gateway.PaymentGateway;
import com.seoulaim.domain.order.gateway.PaymentResult;
import com.seoulaim.domain.order.repository.OrderRepository;
import com.seoulaim.domain.order.repository.PaymentRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

  private final OrderRepository orderRepository;
  private final PaymentRepository paymentRepository;
  private final PaymentGateway paymentGateway;

  /**
   * 토스 결제 승인. 주문 행을 잠근 채 게이트웨이를 호출해 동시 요청이 두 번 승인하지 않게 한다.
   *
   * <p>승인 실패 시 FAILED 상태를 저장한 뒤 PAYMENT_FAILED 를 던지므로 BusinessException 에는 롤백하지 않는다.
   * BusinessException 을 던지는 다른 경로(404, 금액 불일치, 게이트웨이 오류)는 그 전에 쓰기가 없다.
   */
  @Transactional(noRollbackFor = BusinessException.class)
  public PaymentConfirmResponse confirm(UUID userId, UUID orderId, PaymentConfirmRequest request) {
    Order order =
        orderRepository
            .findWithLockByIdAndUserId(orderId, userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    if (request.amount() != order.getFinalAmount()) {
      throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
    }
    if (order.isPaid()) {
      return new PaymentConfirmResponse(order.getId(), order.getStatus());
    }
    // 실패·취소된 주문은 재승인하지 않는다. 앱은 주문을 새로 만든다
    if (!order.isPending()) {
      throw new BusinessException(ErrorCode.PAYMENT_FAILED);
    }
    Payment payment =
        paymentRepository
            .findByOrderId(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

    PaymentResult result =
        paymentGateway.confirm(request.paymentKey(), order.getOrderNumber(), request.amount());

    switch (result) {
      case PaymentResult.Success success -> {
        payment.markDone(
            success.paymentKey(), success.method(), success.approvedAt(), success.rawBody());
        order.markPaid();
        return new PaymentConfirmResponse(order.getId(), order.getStatus());
      }
      case PaymentResult.Failure failure -> {
        payment.markFailed(
            request.paymentKey(), failure.code(), failure.message(), failure.rawBody());
        order.markFailed();
        throw new BusinessException(ErrorCode.PAYMENT_FAILED, failureFields(failure));
      }
    }
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
