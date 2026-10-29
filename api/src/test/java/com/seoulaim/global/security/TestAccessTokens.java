package com.seoulaim.global.security;

import java.util.UUID;
import lombok.RequiredArgsConstructor;

/** 테스트 요청용 Authorization 헤더 값 발급 */
@RequiredArgsConstructor
public class TestAccessTokens {

  private final JwtTokenProvider jwtTokenProvider;

  // "Bearer {access token}" 형식
  public String bearer(UUID userId) {
    return "Bearer " + jwtTokenProvider.issueAccessToken(userId).token();
  }
}
