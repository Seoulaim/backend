package com.seoulaim.domain.place.entity;

// 장소 분류 (어드민·지도 필터용). 기간 한정 여부는 분류가 아닌 운영 기간(PlaceEvent) 유무로 판단
public enum PlaceCategory {
  FANDOM_PLACE,
  GOODS_SHOP,
  ALBUM_SHOP,
  PHOTO_BOOTH,
  ATTRACTION
}
