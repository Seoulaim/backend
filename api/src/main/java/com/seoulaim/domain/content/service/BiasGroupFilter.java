package com.seoulaim.domain.content.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 홈 콘텐츠 최애 그룹 필터 (회원 앱 mocks/rules.js filterByBiasGroups 와 같은 규칙)
 *
 * <ul>
 *   <li>최애 그룹 없음(비로그인·최애 0명) → 전부
 *   <li>그 외 → 최애 그룹 대상이거나 모든 회원 대상(all)인 것
 *   <li>fallback: 최애 그룹 "전용" 항목이 하나도 없으면 전부 (all 만 맞는 건 없음으로 본다)
 * </ul>
 */
final class BiasGroupFilter {

  private BiasGroupFilter() {}

  static <T> List<T> filter(
      List<T> items,
      Function<T, Set<UUID>> groupIdsOf,
      Predicate<T> forEveryone,
      Set<UUID> biasGroupIds,
      boolean fallback) {
    if (biasGroupIds.isEmpty()) {
      return items;
    }
    Predicate<T> forBias = item -> groupIdsOf.apply(item).stream().anyMatch(biasGroupIds::contains);
    if (fallback && items.stream().noneMatch(forBias)) {
      return items;
    }
    return items.stream().filter(forBias.or(forEveryone)).toList();
  }
}
