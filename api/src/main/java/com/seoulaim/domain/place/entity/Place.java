package com.seoulaim.domain.place.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

/**
 * 아이돌 관련 장소
 *
 * <ul>
 *   <li>지도 마커, 일정 항목, 추천 후보의 단위
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

  @Column(name = "name", nullable = false, length = 200)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false, length = 30)
  private PlaceCategory category;

  // false 면 일정 추천·공개 상세에서 제외 (어드민에서만 조회)
  @Column(name = "visible", nullable = false)
  private boolean visible;

  // 옥외 광고처럼 지번이 없는 장소 존재 → nullable. 위치 기준은 location
  @Column(name = "address", length = 500)
  private String address;

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

  @Column(name = "video_key", length = 512)
  private String videoKey;

  // 추천 인기도 가중치. 배치로 갱신하는 비정규화 값
  @Column(name = "visit_count", nullable = false)
  private int visitCount;

  public static Place create(
      String name,
      PlaceCategory category,
      String address,
      Point location,
      String district,
      String phone,
      String homepageUrl) {
    Place place = new Place();
    place.name = name;
    place.category = category;
    place.visible = true;
    place.address = address;
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
      String name,
      PlaceCategory category,
      boolean visible,
      String address,
      Point location,
      String district,
      int avgStayMinutes,
      String phone,
      String homepageUrl) {
    this.name = name;
    this.category = category;
    this.visible = visible;
    this.address = address;
    this.location = location;
    this.district = district;
    this.avgStayMinutes = avgStayMinutes;
    this.phone = phone;
    this.homepageUrl = homepageUrl;
  }

  public void changeVisibility(boolean visible) {
    this.visible = visible;
  }
}
