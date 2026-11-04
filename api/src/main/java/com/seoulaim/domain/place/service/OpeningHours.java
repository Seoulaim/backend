package com.seoulaim.domain.place.service;

import com.seoulaim.domain.place.entity.PlaceOpeningHour;
import java.time.LocalTime;

/**
 * 영업시간 판단
 *
 * <ul>
 *   <li>opensAt·closesAt 중 하나라도 없으면 영업 안 함
 *   <li>closesAt ≤ opensAt → 다음 날 closesAt 까지 (자정 넘김). 00:00 마감 = 24:00, 같으면 24시간
 *   <li>여는 시각 포함, 닫는 시각 제외 (브레이크도 동일)
 *   <li>브레이크는 영업 구간 안 (자정을 걸쳐도 됨). 여는 시각부터 지난 분으로 비교
 * </ul>
 */
public final class OpeningHours {

  private static final int MINUTES_PER_DAY = 24 * 60;

  private OpeningHours() {}

  /** 영업일 시각 조합 검증: 여닫는 시각 필수, 브레이크는 둘 다 있거나 없고 영업 구간 안 */
  public static boolean isValid(
      LocalTime opensAt, LocalTime closesAt, LocalTime breakStartsAt, LocalTime breakEndsAt) {
    if (opensAt == null || closesAt == null) {
      return false;
    }
    if ((breakStartsAt == null) != (breakEndsAt == null)) {
      return false;
    }
    if (breakStartsAt == null) {
      return true;
    }
    int start = offset(opensAt, breakStartsAt);
    int end = offset(opensAt, breakEndsAt);
    return 0 < start && start < end && end < length(opensAt, closesAt);
  }

  // 같은 날 한 구간 기준 (자정 넘김이면 새벽도 그 구간으로 본다)
  public static boolean isOpen(LocalTime opensAt, LocalTime closesAt, LocalTime time) {
    return isOpen(opensAt, closesAt, null, null, time);
  }

  // 브레이크 시간에는 영업 안 함
  public static boolean isOpen(
      LocalTime opensAt,
      LocalTime closesAt,
      LocalTime breakStartsAt,
      LocalTime breakEndsAt,
      LocalTime time) {
    if (opensAt == null || closesAt == null) {
      return false;
    }
    return isOpenAt(opensAt, closesAt, breakStartsAt, breakEndsAt, offset(opensAt, time));
  }

  /**
   * 지금 영업 중인지 (요일 영업시간)
   *
   * <ul>
   *   <li>오늘 행: 오늘 여는 시각 이후
   *   <li>어제 행: 자정을 넘긴 새벽 부분 (예: 어제 16:00 ~ 01:00 이면 오늘 00:30 영업 중)
   *   <li>휴무·없는 행은 무시
   * </ul>
   */
  public static boolean isOpenNow(
      PlaceOpeningHour today, PlaceOpeningHour yesterday, LocalTime time) {
    int minute = minuteOf(time);
    if (isOpenDay(today)
        && minute >= minuteOf(today.getOpensAt())
        && isOpenAt(today, minute - minuteOf(today.getOpensAt()))) {
      return true;
    }
    return isOpenDay(yesterday)
        && isOpenAt(yesterday, MINUTES_PER_DAY - minuteOf(yesterday.getOpensAt()) + minute);
  }

  /**
   * 도착 시각 기준 가장 이른 방문 가능 시각 (그날 행 기준)
   *
   * <ul>
   *   <li>여는 시각 전 도착 → 여는 시각까지 대기 (자정 넘김 영업도 동일)
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
    if (opensAt == null || closesAt == null) {
      return null;
    }
    int visit = Math.max(0, minuteOf(arrival) - minuteOf(opensAt));
    if (isInBreak(opensAt, breakStartsAt, breakEndsAt, visit)) {
      visit = offset(opensAt, breakEndsAt);
    }
    return isOpenAt(opensAt, closesAt, breakStartsAt, breakEndsAt, visit)
        ? opensAt.plusMinutes(visit)
        : null;
  }

  private static boolean isOpenDay(PlaceOpeningHour hour) {
    return hour != null
        && !hour.isClosed()
        && hour.getOpensAt() != null
        && hour.getClosesAt() != null;
  }

  private static boolean isOpenAt(PlaceOpeningHour hour, int offset) {
    return isOpenAt(
        hour.getOpensAt(),
        hour.getClosesAt(),
        hour.getBreakStartsAt(),
        hour.getBreakEndsAt(),
        offset);
  }

  // offset: 여는 시각부터 지난 분
  private static boolean isOpenAt(
      LocalTime opensAt,
      LocalTime closesAt,
      LocalTime breakStartsAt,
      LocalTime breakEndsAt,
      int offset) {
    return offset >= 0
        && offset < length(opensAt, closesAt)
        && !isInBreak(opensAt, breakStartsAt, breakEndsAt, offset);
  }

  private static boolean isInBreak(
      LocalTime opensAt, LocalTime breakStartsAt, LocalTime breakEndsAt, int offset) {
    return breakStartsAt != null
        && breakEndsAt != null
        && offset >= offset(opensAt, breakStartsAt)
        && offset < offset(opensAt, breakEndsAt);
  }

  // 영업 구간 길이(분). 닫는 시각 ≤ 여는 시각이면 다음 날까지, 같으면 24시간
  private static int length(LocalTime opensAt, LocalTime closesAt) {
    int length = offset(opensAt, closesAt);
    return length == 0 ? MINUTES_PER_DAY : length;
  }

  // 여는 시각부터 time 까지 지난 분 (0 ~ 1439, 자정을 넘기면 다음 날로 본다)
  private static int offset(LocalTime opensAt, LocalTime time) {
    return Math.floorMod(minuteOf(time) - minuteOf(opensAt), MINUTES_PER_DAY);
  }

  private static int minuteOf(LocalTime time) {
    return time.getHour() * 60 + time.getMinute();
  }
}
