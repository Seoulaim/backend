package com.seoulaim.domain.place.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

/**
 * 아이돌 관련 장소
 *
 * <ul>
 *   <li>지도 마커, 일정 항목, 추천 후보의 단위
 *   <li>이름·주제·주소는 언어별 저장. 영어 이름 필수, 나머지는 비면 영어로 표시
 *   <li>요일별 영업시간 → PlaceOpeningHour, 운영 기간 → PlaceEvent 로 분리
 *   <li>장소 소개 문구는 아티스트마다 다름 → ArtistPlace 에 저장
 * </ul>
 */
@Getter
@Entity
@Table(
    name = "places",
    indexes = {
      @Index(name = "idx_places_district", columnList = "district"),
      @Index(name = "idx_places_category", columnList = "category")
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseEntity {

  private static final int DEFAULT_STAY_MINUTES = 60;

  @Column(name = "name_en", nullable = false, length = 200)
  private String nameEn;

  @Column(name = "name_ko", length = 200)
  private String nameKo;

  @Column(name = "name_ja", length = 200)
  private String nameJa;

  @Column(name = "name_zh", length = 200)
  private String nameZh;

  // 주제: 일정 목록에서 장소 이름 아래 한 줄 소개
  @Column(name = "summary_en", length = 200)
  private String summaryEn;

  @Column(name = "summary_ko", length = 200)
  private String summaryKo;

  @Column(name = "summary_ja", length = 200)
  private String summaryJa;

  @Column(name = "summary_zh", length = 200)
  private String summaryZh;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false, length = 30)
  private PlaceCategory category;

  // false 면 일정 추천·공개 상세에서 제외 (어드민에서만 조회)
  @Column(name = "visible", nullable = false)
  private boolean visible;

  // 좌표 찾기 기준 주소
  @Column(name = "address_ko", length = 500)
  private String addressKo;

  @Column(name = "address_en", length = 500)
  private String addressEn;

  @Column(name = "address_ja", length = 500)
  private String addressJa;

  @Column(name = "address_zh", length = 500)
  private String addressZh;

  // SRID 4326 (x = 경도, y = 위도). geography 타입 → 거리 연산 결과가 미터 단위
  // GIST 인덱스는 마이그레이션 SQL 에서 생성
  @Column(name = "location", nullable = false, columnDefinition = "geography(Point,4326)")
  private Point location;

  // 구·동 단위 지역 검색용
  @Column(name = "district", length = 50)
  private String district;

  // 추천 시 다음 장소 도착 시각 계산에 사용
  @Column(name = "avg_stay_minutes", nullable = false)
  private int avgStayMinutes;

  @Column(name = "phone", length = 50)
  private String phone;

  // 외부 사이트 URL (S3 키 아님)
  @Column(name = "homepage_url", length = 512)
  private String homepageUrl;

  // 비어 있으면 클라이언트가 장소명 검색 링크 사용
  @Column(name = "naver_map_url", length = 512)
  private String naverMapUrl;

  // 팬덤 플레이스 근거 영상 URL 목록
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "reference_links", nullable = false, columnDefinition = "jsonb")
  private List<String> referenceLinks = new ArrayList<>();

  @Column(name = "video_key", length = 512)
  private String videoKey;

  // 추천 인기도 가중치. 배치로 갱신하는 비정규화 값
  @Column(name = "visit_count", nullable = false)
  private int visitCount;

  // 한국어 이름·주소만 있는 단순 등록 (테스트·시드용). 영어 이름도 같은 값
  public static Place create(
      String name,
      PlaceCategory category,
      String address,
      Point location,
      String district,
      String phone,
      String homepageUrl) {
    Place place = new Place();
    place.nameEn = name;
    place.nameKo = name;
    place.category = category;
    place.visible = true;
    place.addressKo = address;
    place.location = location;
    place.district = district;
    place.avgStayMinutes = DEFAULT_STAY_MINUTES;
    place.phone = phone;
    place.homepageUrl = homepageUrl;
    place.visitCount = 0;
    return place;
  }

  // 어드민 수정. 전체 교체
  public void update(
      LocalizedText name,
      LocalizedText summary,
      PlaceCategory category,
      boolean visible,
      LocalizedText address,
      Point location,
      String district,
      int avgStayMinutes,
      String phone,
      String homepageUrl,
      String naverMapUrl,
      List<String> referenceLinks) {
    this.nameEn = name.en();
    this.nameKo = name.ko();
    this.nameJa = name.ja();
    this.nameZh = name.zh();
    this.summaryEn = summary.en();
    this.summaryKo = summary.ko();
    this.summaryJa = summary.ja();
    this.summaryZh = summary.zh();
    this.category = category;
    this.visible = visible;
    this.addressEn = address.en();
    this.addressKo = address.ko();
    this.addressJa = address.ja();
    this.addressZh = address.zh();
    this.location = location;
    this.district = district;
    this.avgStayMinutes = avgStayMinutes;
    this.phone = phone;
    this.homepageUrl = homepageUrl;
    this.naverMapUrl = naverMapUrl;
    this.referenceLinks = new ArrayList<>(referenceLinks);
  }

  public void changeVisibility(boolean visible) {
    this.visible = visible;
  }

  // 기본 표시 이름: 한국어, 없으면 영어
  public String getName() {
    return nameKo == null || nameKo.isBlank() ? nameEn : nameKo;
  }

  // 기본 표시 주소: 한국어, 없으면 영어
  public String getAddress() {
    return addressKo == null || addressKo.isBlank() ? addressEn : addressKo;
  }

  public LocalizedText getNames() {
    return new LocalizedText(nameEn, nameKo, nameJa, nameZh);
  }

  public LocalizedText getSummaries() {
    return new LocalizedText(summaryEn, summaryKo, summaryJa, summaryZh);
  }

  public LocalizedText getAddresses() {
    return new LocalizedText(addressEn, addressKo, addressJa, addressZh);
  }
}
