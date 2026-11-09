package com.seoulaim.domain.diary.repository;

import java.util.UUID;

/** 카테고리별 스티커 수 projection. */
public interface CategoryStickerCount {

  UUID getCategoryId();

  long getCount();
}
