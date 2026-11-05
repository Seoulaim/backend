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
  AUTH_INVALID_PASSWORD(
      HttpStatus.BAD_REQUEST, "AUTH_INVALID_PASSWORD", "비밀번호는 8~64자, 영문·숫자를 각 1자 이상 포함해야 합니다"),
  AUTH_MAIL_SEND_FAILED(
      HttpStatus.SERVICE_UNAVAILABLE, "AUTH_MAIL_SEND_FAILED", "인증 메일 발송에 실패했습니다"),

  // USER
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다"),
  USER_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "USER_EMAIL_DUPLICATED", "이미 사용 중인 이메일입니다"),
  USER_SUSPENDED(HttpStatus.FORBIDDEN, "USER_SUSPENDED", "이용이 정지된 계정입니다"),
  USER_INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "USER_INVALID_NICKNAME", "닉네임은 1~20자여야 합니다"),

  // ARTIST
  ARTIST_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "ARTIST_GROUP_NOT_FOUND", "그룹을 찾을 수 없습니다"),
  ARTIST_NOT_FOUND(HttpStatus.NOT_FOUND, "ARTIST_NOT_FOUND", "아티스트를 찾을 수 없습니다"),
  ARTIST_INVALID_CURSOR(HttpStatus.BAD_REQUEST, "ARTIST_INVALID_CURSOR", "잘못된 커서 값입니다"),
  ARTIST_NOT_IN_GROUP(HttpStatus.BAD_REQUEST, "ARTIST_NOT_IN_GROUP", "해당 그룹의 멤버가 아닙니다"),

  // PLACE
  PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE_NOT_FOUND", "장소를 찾을 수 없습니다"),
  PLACE_KEYWORD_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE_KEYWORD_NOT_FOUND", "키워드를 찾을 수 없습니다"),
  PLACE_KEYWORD_DUPLICATED(HttpStatus.CONFLICT, "PLACE_KEYWORD_DUPLICATED", "이미 있는 키워드 코드입니다"),
  PLACE_KEYWORD_INVALID_CODE(
      HttpStatus.BAD_REQUEST, "PLACE_KEYWORD_INVALID_CODE", "영어 이름으로 키워드 코드를 만들 수 없습니다"),
  PLACE_KEYWORD_INVALID_ORDER(
      HttpStatus.BAD_REQUEST, "PLACE_KEYWORD_INVALID_ORDER", "모든 키워드를 한 번씩 포함해야 합니다"),
  PLACE_INVALID_CURSOR(HttpStatus.BAD_REQUEST, "PLACE_INVALID_CURSOR", "잘못된 커서 값입니다"),
  PLACE_IN_USE(HttpStatus.CONFLICT, "PLACE_IN_USE", "일정에 포함된 장소는 삭제할 수 없습니다"),
  PLACE_INVALID_OPERATING_PERIOD(
      HttpStatus.BAD_REQUEST, "PLACE_INVALID_OPERATING_PERIOD", "운영 기간이 올바르지 않습니다"),
  PLACE_INVALID_OPENING_HOURS(
      HttpStatus.BAD_REQUEST, "PLACE_INVALID_OPENING_HOURS", "영업시간이 올바르지 않습니다"),

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
  PAYMENT_ALREADY_PAID(HttpStatus.CONFLICT, "PAYMENT_ALREADY_PAID", "이미 결제가 완료되었습니다"),
  PAYMENT_CONSENT_REQUIRED(
      HttpStatus.BAD_REQUEST, "PAYMENT_CONSENT_REQUIRED", "환불 규정에 동의해야 결제할 수 있습니다"),
  PAYMENT_REFUND_FAILED(HttpStatus.BAD_GATEWAY, "PAYMENT_REFUND_FAILED", "결제 취소(환불)에 실패했습니다"),

  // DIARY
  DIARY_NOT_FOUND(HttpStatus.NOT_FOUND, "DIARY_NOT_FOUND", "다이어리를 찾을 수 없습니다"),
  DIARY_ORDER_NOT_ELIGIBLE(
      HttpStatus.BAD_REQUEST, "DIARY_ORDER_NOT_ELIGIBLE", "다이어리를 제출할 수 없는 주문입니다"),
  DIARY_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "DIARY_ALREADY_SUBMITTED", "이미 제출된 다이어리입니다"),
  DIARY_INVALID_PDF(HttpStatus.BAD_REQUEST, "DIARY_INVALID_PDF", "다이어리 PDF 파일이 올바르지 않습니다"),
  DIARY_CONTENT_TOO_LARGE(HttpStatus.BAD_REQUEST, "DIARY_CONTENT_TOO_LARGE", "다이어리 편집 데이터가 너무 큽니다"),
  DIARY_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "DIARY_TEMPLATE_NOT_FOUND", "다이어리 템플릿을 찾을 수 없습니다"),
  DIARY_STICKER_NOT_FOUND(HttpStatus.NOT_FOUND, "DIARY_STICKER_NOT_FOUND", "다이어리 스티커를 찾을 수 없습니다"),
  DIARY_FINALIZED(HttpStatus.CONFLICT, "DIARY_FINALIZED", "완성된 다이어리는 수정할 수 없습니다"),
  DIARY_PAYMENT_REQUIRED(HttpStatus.PAYMENT_REQUIRED, "DIARY_PAYMENT_REQUIRED", "결제 후 저장할 수 있습니다"),
  DIARY_INVALID_NAME(HttpStatus.BAD_REQUEST, "DIARY_INVALID_NAME", "다이어리 이름은 1~30자여야 합니다"),
  DIARY_NOT_FINISHED(HttpStatus.CONFLICT, "DIARY_NOT_FINISHED", "아직 완성되지 않은 다이어리입니다"),

  // SURVEY
  SURVEY_ALREADY_ANSWERED(HttpStatus.CONFLICT, "SURVEY_ALREADY_ANSWERED", "이미 응답한 설문입니다"),
  SURVEY_INVALID(HttpStatus.BAD_REQUEST, "SURVEY_INVALID", "설문 내용이 올바르지 않습니다"),
  SURVEY_QUESTION_HAS_ANSWERS(
      HttpStatus.CONFLICT, "SURVEY_QUESTION_HAS_ANSWERS", "응답이 있는 질문은 삭제할 수 없습니다. 숨김 처리해주세요"),
  SURVEY_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SURVEY_QUESTION_NOT_FOUND", "설문 질문을 찾을 수 없습니다"),

  // CONTENT
  CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CONTENT_NOT_FOUND", "콘텐츠를 찾을 수 없습니다"),
  CONTENT_DEFAULT_BANNER(HttpStatus.CONFLICT, "CONTENT_DEFAULT_BANNER", "기본 배너는 끄거나 삭제할 수 없습니다"),
  CONTENT_INVALID_GROUP(HttpStatus.BAD_REQUEST, "CONTENT_INVALID_GROUP", "존재하지 않는 그룹 또는 멤버입니다"),

  // INQUIRY
  INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "INQUIRY_NOT_FOUND", "문의를 찾을 수 없습니다"),
  INQUIRY_INVALID(HttpStatus.BAD_REQUEST, "INQUIRY_INVALID", "문의 내용이 올바르지 않습니다"),
  INQUIRY_REFUND_ALREADY_REQUESTED(
      HttpStatus.CONFLICT, "INQUIRY_REFUND_ALREADY_REQUESTED", "이미 환불을 요청했거나 환불된 결제입니다"),
  INQUIRY_ALREADY_DECIDED(HttpStatus.CONFLICT, "INQUIRY_ALREADY_DECIDED", "이미 처리된 환불 요청입니다"),
  INQUIRY_POLICY_EXCEPTION_REQUIRED(
      HttpStatus.BAD_REQUEST,
      "INQUIRY_POLICY_EXCEPTION_REQUIRED",
      "환불 정책 밖 요청은 예외 사유가 있어야 승인할 수 있습니다"),
  ;

  private final HttpStatus status;
  private final String code;
  private final String message;
}
