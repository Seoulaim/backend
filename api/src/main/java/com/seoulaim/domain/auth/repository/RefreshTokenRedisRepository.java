package com.seoulaim.domain.auth.repository;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 유효한 refresh token 목록 (Redis)
 *
 * <ul>
 *   <li>키 존재 = 사용 가능, 삭제 = 폐기
 *   <li>tokenId(jti) 단위 → 기기별 로그인 유지
 * </ul>
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRedisRepository {

  private static final String KEY_PREFIX = "auth:refresh-token:";

  private final StringRedisTemplate redisTemplate;

  public void save(UUID userId, String tokenId, Duration ttl) {
    redisTemplate.opsForValue().set(key(userId, tokenId), "1", ttl);
  }

  // 삭제 성공 여부 반환 → false 면 이미 폐기된 토큰
  public boolean delete(UUID userId, String tokenId) {
    return Boolean.TRUE.equals(redisTemplate.delete(key(userId, tokenId)));
  }

  private String key(UUID userId, String tokenId) {
    return KEY_PREFIX + userId + ":" + tokenId;
  }
}
