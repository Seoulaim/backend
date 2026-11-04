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
 * 장소 키워드 (카페, 맛집, 포토스팟 등)
 *
 * <ul>
 *   <li>일정 생성 시 취향 조건, 추천 알고리즘 입력
 *   <li>운영자가 추가·삭제 → enum 대신 테이블로 관리
 *   <li>code: 고유 식별 코드, 등록 후 변경 불가
 *   <li>이름: 영어 필수, 나머지 언어는 비면 영어로 표시
 * </ul>
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

  @Column(name = "name_en", nullable = false, length = 100)
  private String nameEn;

  @Column(name = "name_ko", length = 100)
  private String nameKo;

  @Column(name = "name_ja", length = 100)
  private String nameJa;

  @Column(name = "name_zh", length = 100)
  private String nameZh;

  // 키워드 선택 화면 노출 순서
  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  public static Keyword create(String code, LocalizedText name, int displayOrder) {
    Keyword keyword = new Keyword();
    keyword.code = code;
    keyword.rename(name);
    keyword.displayOrder = displayOrder;
    return keyword;
  }

  // 영어 이름 = 코드 (테스트·시드용)
  public static Keyword create(String code, int displayOrder) {
    return create(code, new LocalizedText(code, null, null, null), displayOrder);
  }

  public void rename(LocalizedText name) {
    this.nameEn = name.en();
    this.nameKo = name.ko();
    this.nameJa = name.ja();
    this.nameZh = name.zh();
  }

  public void changeDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }

  public LocalizedText getNames() {
    return new LocalizedText(nameEn, nameKo, nameJa, nameZh);
  }
}
