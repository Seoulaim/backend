package com.seoulaim.domain.diary.repository;

import java.util.UUID;

/** 회원별 집계 projection. */
public interface UserCount {

  UUID getUserId();

  long getCount();
}
