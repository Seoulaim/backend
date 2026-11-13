package com.seoulaim.global.common;

/**
 * 네이티브 집계 쿼리의 KST 날짜별 건수 행 (대시보드 일별 추이).
 *
 * <p>날짜는 쿼리에서 {@code to_char(... AT TIME ZONE 'Asia/Seoul', 'YYYY-MM-DD')} 로 만든 문자열이다. JDBC 날짜 타입
 * 변환 차이를 피하려고 문자열로 받는다.
 */
public interface KstDateCount {

  String getDay();

  long getCount();
}
