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
  AUTH_OTP_RESEND_TOO_SOON(
      HttpStatus.TOO_MANY_REQUESTS, "AUTH_OTP_RESEND_TOO_SOON", "잠시 후 다시 요청해주세요"),
  AUTH_OTP_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_OTP_MISMATCH", "인증번호가 일치하지 않습니다"),
  AUTH_OTP_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH_OTP_EXPIRED", "인증번호가 만료되었습니다. 다시 요청해주세요"),
  AUTH_OTP_ATTEMPTS_EXCEEDED(
      HttpStatus.TOO_MANY_REQUESTS, "AUTH_OTP_ATTEMPTS_EXCEEDED", "인증 시도 횟수를 초과했습니다. 다시 요청해주세요"),
  AUTH_EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "AUTH_EMAIL_NOT_VERIFIED", "이메일 인증이 필요합니다"),
  AUTH_MAIL_SEND_FAILED(
      HttpStatus.SERVICE_UNAVAILABLE, "AUTH_MAIL_SEND_FAILED", "인증 메일 발송에 실패했습니다"),

  // USER
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다"),
  USER_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "USER_EMAIL_DUPLICATED", "이미 사용 중인 이메일입니다"),
  USER_SUSPENDED(HttpStatus.FORBIDDEN, "USER_SUSPENDED", "이용이 정지된 계정입니다"),

  // ARTIST
  ARTIST_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "ARTIST_GROUP_NOT_FOUND", "그룹을 찾을 수 없습니다"),
  ARTIST_NOT_FOUND(HttpStatus.NOT_FOUND, "ARTIST_NOT_FOUND", "아티스트를 찾을 수 없습니다"),
  ARTIST_INVALID_CURSOR(HttpStatus.BAD_REQUEST, "ARTIST_INVALID_CURSOR", "잘못된 커서 값입니다"),

  // PLACE
  PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE_NOT_FOUND", "장소를 찾을 수 없습니다"),
  PLACE_KEYWORD_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE_KEYWORD_NOT_FOUND", "키워드를 찾을 수 없습니다"),

  // PLAN
  PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "일정을 찾을 수 없습니다"),
  PLAN_INVALID_PERIOD(HttpStatus.BAD_REQUEST, "PLAN_INVALID_PERIOD", "여행 기간이 올바르지 않습니다"),
  PLAN_VISIT_DATE_OUT_OF_PERIOD(
      HttpStatus.BAD_REQUEST, "PLAN_VISIT_DATE_OUT_OF_PERIOD", "투어 날짜는 여행 기간 안에서 선택해야 합니다"),
  PLAN_NO_CANDIDATE_PLACE(
      HttpStatus.UNPROCESSABLE_ENTITY,
      "PLAN_NO_CANDIDATE_PLACE",
      "조건에 맞는 장소가 없습니다. bias 나 키워드를 바꿔주세요"),
  PLAN_INVALID_CURSOR(HttpStatus.BAD_REQUEST, "PLAN_INVALID_CURSOR", "잘못된 커서 값입니다"),

  // GOODS
  GOODS_NOT_FOUND(HttpStatus.NOT_FOUND, "GOODS_NOT_FOUND", "상품을 찾을 수 없습니다"),
  GOODS_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "GOODS_CATEGORY_NOT_FOUND", "상품 카테고리를 찾을 수 없습니다"),
  GOODS_INVALID_CURSOR(HttpStatus.BAD_REQUEST, "GOODS_INVALID_CURSOR", "잘못된 커서 값입니다"),
  ;

  private final HttpStatus status;
  private final String code;
  private final String message;
}
