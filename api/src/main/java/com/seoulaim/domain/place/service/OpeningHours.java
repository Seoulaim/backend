package com.seoulaim.domain.place.service;

import java.time.LocalTime;

/**
 * 영업시간 판단
 *
 * <ul>
 *   <li>opensAt·closesAt 중 하나라도 없으면 영업 안 함
 *   <li>closesAt < opensAt → 다음 날 새벽 마감
 *   <li>여는 시각 포함, 닫는 시각 제외
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
}
