package com.seoulaim.domain.order.controller;

import com.seoulaim.domain.order.dto.CreateOrderRequest;
import com.seoulaim.domain.order.dto.CreateOrderResponse;
import com.seoulaim.domain.order.dto.OrderDetailResponse;
import com.seoulaim.domain.order.dto.OrderSummaryResponse;
import com.seoulaim.domain.order.dto.PaymentConfirmRequest;
import com.seoulaim.domain.order.dto.PaymentConfirmResponse;
import com.seoulaim.domain.order.service.OrderService;
import com.seoulaim.domain.order.service.PaymentService;
import com.seoulaim.global.common.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Order")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

  private final OrderService orderService;
  private final PaymentService paymentService;

  @Operation(summary = "주문 생성 (금액은 서버 계산, 결제 대기 상태)")
  @Parameter(
      name = "X-User-Id",
      in = ParameterIn.HEADER,
      required = true,
      description = "auth 구현 전 임시 유저 식별 헤더 (UUID)")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CreateOrderResponse createOrder(
      @Parameter(hidden = true) @CurrentUserId UUID userId,
      @Valid @RequestBody CreateOrderRequest request) {
    return orderService.create(userId, request);
  }

  @Operation(summary = "토스 결제 승인 (결제창 성공 콜백 후 호출)")
  @Parameter(
      name = "X-User-Id",
      in = ParameterIn.HEADER,
      required = true,
      description = "auth 구현 전 임시 유저 식별 헤더 (UUID)")
  @PostMapping("/{orderId}/payments/confirm")
  public PaymentConfirmResponse confirmPayment(
      @Parameter(hidden = true) @CurrentUserId UUID userId,
      @PathVariable UUID orderId,
      @Valid @RequestBody PaymentConfirmRequest request) {
    return paymentService.confirm(userId, orderId, request);
  }

  @Operation(summary = "내 주문 목록 (최신순, 최대 50개)")
  @Parameter(
      name = "X-User-Id",
      in = ParameterIn.HEADER,
      required = true,
      description = "auth 구현 전 임시 유저 식별 헤더 (UUID)")
  @GetMapping
  public List<OrderSummaryResponse> getMyOrders(
      @Parameter(hidden = true) @CurrentUserId UUID userId) {
    return orderService.getMyOrders(userId);
  }

  @Operation(summary = "내 주문 상세")
  @Parameter(
      name = "X-User-Id",
      in = ParameterIn.HEADER,
      required = true,
      description = "auth 구현 전 임시 유저 식별 헤더 (UUID)")
  @GetMapping("/{orderId}")
  public OrderDetailResponse getMyOrder(
      @Parameter(hidden = true) @CurrentUserId UUID userId, @PathVariable UUID orderId) {
    return orderService.getMyOrder(userId, orderId);
  }
}
