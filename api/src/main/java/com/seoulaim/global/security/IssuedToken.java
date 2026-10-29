package com.seoulaim.global.security;

import java.time.Instant;

/** 발급된 토큰. tokenId 는 refresh token 의 Redis 저장 키로 사용 */
public record IssuedToken(String token, String tokenId, Instant expiresAt) {}
