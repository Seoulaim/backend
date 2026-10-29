package com.seoulaim.domain.place.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장소 키워드 (카페, 맛집, 포토스팟 등) - 일정 생성 시 취향 조건, 추천 알고리즘 입력 - 운영자가 추가·삭제 → enum 대신 테이블로 관리 - 표시 문구 대신 코드
 * 저장, 언어별 문구는 클라이언트에서 번역
 */
@Getter
@Entity
@Table(
    name = "keywords",
    uniqueConstraints = @UniqueConstraint(name = "uk_keywords_code", columnNames = "code"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Keyword extends BaseEntity {

  // 예: CAFE, RESTAURANT, PHOTO_SPOT
  @Column(name = "code", nullable = false, length = 50)
  private String code;

  // 키워드 선택 화면 노출 순서
  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  public static Keyword create(String code, int displayOrder) {
    Keyword keyword = new Keyword();
    keyword.code = code;
    keyword.displayOrder = displayOrder;
    return keyword;
  }
}
