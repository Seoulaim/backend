package com.seoulaim.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  // COMMON
  COMMON_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_INVALID_REQUEST", "잘못된 요청입니다"),
  COMMON_METHOD_NOT_ALLOWED(
      HttpStatus.METHOD_NOT_ALLOWED, "COMMON_METHOD_NOT_ALLOWED", "지원하지 않는 요청 방식입니다"),
  COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_INTERNAL_ERROR", "서버 오류가 발생했습니다"),

  // AUTH
  AUTH_INVALID_CREDENTIALS(
      HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다"),
  AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_TOKEN", "유효하지 않은 토큰입니다"),
  AUTH_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_EXPIRED_TOKEN", "만료된 토큰입니다"),
  AUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_ACCESS_DENIED", "접근 권한이 없습니다"),

  // USER
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다"),
  USER_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "USER_EMAIL_DUPLICATED", "이미 사용 중인 이메일입니다"),

  // GOODS
  GOODS_NOT_FOUND(HttpStatus.NOT_FOUND, "GOODS_NOT_FOUND", "상품을 찾을 수 없습니다"),
  GOODS_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "GOODS_CATEGORY_NOT_FOUND", "상품 카테고리를 찾을 수 없습니다"),
  GOODS_INVALID_CURSOR(HttpStatus.BAD_REQUEST, "GOODS_INVALID_CURSOR", "잘못된 커서 값입니다"),
  GOODS_IN_USE(HttpStatus.CONFLICT, "GOODS_IN_USE", "주문 이력이 있는 상품은 삭제할 수 없습니다"),

  // FILE
  FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "파일을 찾을 수 없습니다"),
  FILE_INVALID_CONTENT_TYPE(
      HttpStatus.BAD_REQUEST, "FILE_INVALID_CONTENT_TYPE", "허용되지 않는 파일 형식입니다"),
  FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE", "파일 크기가 허용 범위를 초과했습니다"),
  FILE_INVALID_DOMAIN(HttpStatus.BAD_REQUEST, "FILE_INVALID_DOMAIN", "잘못된 파일 도메인입니다"),

  // ORDER
  ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "주문을 찾을 수 없습니다"),
  ORDER_ADDRESS_NOT_SEOUL(
      HttpStatus.BAD_REQUEST, "ORDER_ADDRESS_NOT_SEOUL", "서울 지역 주소로만 배송할 수 있습니다"),
  ORDER_INVALID_ITEM(HttpStatus.BAD_REQUEST, "ORDER_INVALID_ITEM", "주문할 수 없는 상품이 포함되어 있습니다"),

  // PAYMENT
  PAYMENT_AMOUNT_MISMATCH(
      HttpStatus.BAD_REQUEST, "PAYMENT_AMOUNT_MISMATCH", "결제 금액이 주문 금액과 일치하지 않습니다"),
  PAYMENT_FAILED(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_FAILED", "결제에 실패했습니다"),
  PAYMENT_GATEWAY_ERROR(HttpStatus.BAD_GATEWAY, "PAYMENT_GATEWAY_ERROR", "결제사 통신 중 오류가 발생했습니다"),
  ;

  private final HttpStatus status;
  private final String code;
  private final String message;
}
