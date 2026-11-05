package com.seoulaim.domain.inquiry.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** 환불 처리 기한 (환불 정책 제6조 2항: 영업일 기준 3일). 주말만 건너뛰고 공휴일은 반영하지 않는다(어드민 lib 와 동일). */
final class RefundDeadline {

  static final int BUSINESS_DAYS = 3;
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private RefundDeadline() {}

  static LocalDate of(Instant requestedAt) {
    if (requestedAt == null) {
      return null;
    }
    LocalDate date = LocalDate.ofInstant(requestedAt, KST);
    int left = BUSINESS_DAYS;
    while (left > 0) {
      date = date.plusDays(1);
      if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
        left--;
      }
    }
    return date;
  }
}
