package com.seoulaim.global.util;

import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.Map;

/** 어드민 목록 page/size 검증. page 는 0부터, size 는 1~{@value #MAX_SIZE}. */
public final class AdminPaging {

  public static final int MAX_SIZE = 200;

  private AdminPaging() {}

  /** 범위 밖이면 COMMON_INVALID_REQUEST(400). */
  public static void validate(int page, int size) {
    if (page < 0) {
      throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST, Map.of("page", "0 이상이어야 합니다"));
    }
    if (size < 1 || size > MAX_SIZE) {
      throw new BusinessException(
          ErrorCode.COMMON_INVALID_REQUEST, Map.of("size", "1~" + MAX_SIZE + " 사이여야 합니다"));
    }
  }
}
