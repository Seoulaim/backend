package com.seoulaim.domain.plan.service;

import java.util.Collection;

/**
 * 일정 추천용 장소 종류 (장소 키워드로 판단)
 *
 * <ul>
 *   <li>키워드가 여럿이면 BAR → RESTAURANT → CAFE 순으로 우선
 *   <li>BIRTHDAY_CAFE 는 팬덤 장소라 카페 개수 제한 대상 아님 (OTHER)
 *   <li>OTHER 끼리는 연속 배치 허용
 * </ul>
 */
public enum PlaceKind {
  RESTAURANT,
  CAFE,
  BAR,
  OTHER;

  static final String RESTAURANT_CODE = "RESTAURANT";
  static final String CAFE_CODE = "CAFE";
  static final String BAR_CODE = "BAR";
  static final String PREMIUM_CODE = "PREMIUM";

  public static PlaceKind from(Collection<String> keywordCodes) {
    if (keywordCodes.contains(BAR_CODE)) {
      return BAR;
    }
    if (keywordCodes.contains(RESTAURANT_CODE)) {
      return RESTAURANT;
    }
    if (keywordCodes.contains(CAFE_CODE)) {
      return CAFE;
    }
    return OTHER;
  }

  public static boolean isPremium(Collection<String> keywordCodes) {
    return keywordCodes.contains(PREMIUM_CODE);
  }

  // 식당·카페·술집은 같은 종류 연속 배치 금지
  boolean isFood() {
    return this != OTHER;
  }
}
