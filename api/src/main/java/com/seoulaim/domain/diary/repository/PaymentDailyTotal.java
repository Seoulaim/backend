package com.seoulaim.domain.diary.repository;

import java.math.BigDecimal;

/** 결제 일별 합계 행 (KST 날짜 × 통화 × 결제수단). 날짜는 'YYYY-MM-DD' 문자열, 통화·수단은 enum 이름. */
public interface PaymentDailyTotal {

  String getDay();

  String getCurrency();

  String getMethod();

  long getCount();

  BigDecimal getAmount();
}
