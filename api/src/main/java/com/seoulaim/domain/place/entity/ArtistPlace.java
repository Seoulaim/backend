package com.seoulaim.domain.place.entity;

import com.seoulaim.domain.artist.entity.Artist;
import com.seoulaim.domain.artist.entity.ArtistGroup;
import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 아티스트 ↔ 장소 연결 - 장소의 아티스트 태그, 소개 문구, bias 기반 추천 근거 - 그룹 전체 관련 장소(그룹 촬영지 등)는 artist 를 비워 그룹 단위로 연결 -
 * 중복 방지: artist null 여부별 partial unique 인덱스 2개 (마이그레이션 SQL)
 */
@Getter
@Entity
@Table(
    name = "artist_places",
    indexes = {
      @Index(name = "idx_artist_places_place_id", columnList = "place_id"),
      @Index(name = "idx_artist_places_group_id", columnList = "artist_group_id"),
      @Index(name = "idx_artist_places_artist_id", columnList = "artist_id")
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtistPlace extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "place_id", nullable = false)
  private Place place;

  // 아티스트 단위 연결에서도 채움 → 그룹 태그 표시·그룹 검색 시 artists 조인 불필요
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "artist_group_id", nullable = false)
  private ArtistGroup artistGroup;

  // null 이면 그룹 단위 연결
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "artist_id")
  private Artist artist;

  // 같은 장소라도 아티스트마다 사연이 다름 → 연결 단위로 저장
  @Column(name = "description", length = 500)
  private String description;

  public static ArtistPlace ofGroup(Place place, ArtistGroup artistGroup, String description) {
    ArtistPlace artistPlace = new ArtistPlace();
    artistPlace.place = place;
    artistPlace.artistGroup = artistGroup;
    artistPlace.description = description;
    return artistPlace;
  }

  // 그룹을 아티스트에서 가져옴 → 아티스트·그룹 불일치 방지
  public static ArtistPlace ofArtist(Place place, Artist artist, String description) {
    ArtistPlace artistPlace = new ArtistPlace();
    artistPlace.place = place;
    artistPlace.artistGroup = artist.getArtistGroup();
    artistPlace.artist = artist;
    artistPlace.description = description;
    return artistPlace;
  }
}
