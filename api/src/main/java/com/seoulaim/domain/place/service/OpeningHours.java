package com.seoulaim.domain.place.service;

import java.time.LocalTime;

/**
 * 영업시간 판단
 *
 * <ul>
 *   <li>opensAt·closesAt 중 하나라도 없으면 영업 안 함
 *   <li>closesAt < opensAt → 다음 날 새벽 마감
 *   <li>여는 시각 포함, 닫는 시각 제외 (브레이크도 동일)
 * </ul>
 */
public final class OpeningHours {

  private OpeningHours() {}

  public static boolean isOpen(LocalTime opensAt, LocalTime closesAt, LocalTime time) {
    if (opensAt == null || closesAt == null) {
      return false;
    }
    if (closesAt.isAfter(opensAt)) {
      return !time.isBefore(opensAt) && time.isBefore(closesAt);
    }
    return !time.isBefore(opensAt) || time.isBefore(closesAt);
  }

  // 브레이크 시간에는 영업 안 함
  public static boolean isOpen(
      LocalTime opensAt,
      LocalTime closesAt,
      LocalTime breakStartsAt,
      LocalTime breakEndsAt,
      LocalTime time) {
    return isOpen(opensAt, closesAt, time) && !isInBreak(breakStartsAt, breakEndsAt, time);
  }

  /**
   * 도착 시각 기준 가장 이른 방문 가능 시각
   *
   * <ul>
   *   <li>여는 시각 전 도착 → 여는 시각까지 대기 (자정 넘김 영업은 대기 없음)
   *   <li>브레이크 중 도착 → 브레이크 끝까지 대기
   *   <li>대기 후에도 영업 중이 아니면 null
   * </ul>
   */
  public static LocalTime earliestVisitTime(
      LocalTime opensAt,
      LocalTime closesAt,
      LocalTime breakStartsAt,
      LocalTime breakEndsAt,
      LocalTime arrival) {
    LocalTime visit = arrival;
    if (opensAt != null
        && closesAt != null
        && closesAt.isAfter(opensAt)
        && visit.isBefore(opensAt)) {
      visit = opensAt;
    }
    if (isInBreak(breakStartsAt, breakEndsAt, visit)) {
      visit = breakEndsAt;
    }
    return isOpen(opensAt, closesAt, breakStartsAt, breakEndsAt, visit) ? visit : null;
  }

  private static boolean isInBreak(LocalTime breakStartsAt, LocalTime breakEndsAt, LocalTime time) {
    return breakStartsAt != null
        && breakEndsAt != null
        && !time.isBefore(breakStartsAt)
        && time.isBefore(breakEndsAt);
  }
}
