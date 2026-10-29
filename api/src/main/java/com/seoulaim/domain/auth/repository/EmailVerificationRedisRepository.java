package com.seoulaim.domain.auth.repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 이메일 인증 상태 Redis 저장소
 *
 * <ul>
 *   <li>code: 발송한 인증번호 (TTL 5분)
 *   <li>attempts: 인증번호 실패 횟수
 *   <li>cooldown: 재발송 제한 표시 (TTL 60초)
 *   <li>token: 인증 완료 → 회원가입용 토큰과 이메일 매핑 (TTL 30분)
 * </ul>
 */
@Repository
@RequiredArgsConstructor
public class EmailVerificationRedisRepository {

  private static final String PREFIX = "auth:email-verification:";
  private static final String CODE_KEY = PREFIX + "code:";
  private static final String ATTEMPTS_KEY = PREFIX + "attempts:";
  private static final String COOLDOWN_KEY = PREFIX + "cooldown:";
  private static final String TOKEN_KEY = PREFIX + "token:";

  private final StringRedisTemplate redisTemplate;

  // SET NX EX → 동시 요청에도 하나만 성공
  public boolean tryAcquireResendCooldown(String email, Duration cooldown) {
    return Boolean.TRUE.equals(
        redisTemplate.opsForValue().setIfAbsent(COOLDOWN_KEY + email, "1", cooldown));
  }

  public void deleteResendCooldown(String email) {
    redisTemplate.delete(COOLDOWN_KEY + email);
  }

  // 재발송 시 이전 실패 횟수 초기화
  public void saveCode(String email, String code, Duration ttl) {
    redisTemplate.opsForValue().set(CODE_KEY + email, code, ttl);
    redisTemplate.delete(ATTEMPTS_KEY + email);
  }

  public Optional<String> findCode(String email) {
    return Optional.ofNullable(redisTemplate.opsForValue().get(CODE_KEY + email));
  }

  // 첫 실패 시 TTL 설정 → 인증번호와 함께 만료
  public long increaseAttempts(String email, Duration ttl) {
    String key = ATTEMPTS_KEY + email;
    Long attempts = redisTemplate.opsForValue().increment(key);
    if (attempts != null && attempts == 1) {
      redisTemplate.expire(key, ttl);
    }
    return attempts == null ? 0 : attempts;
  }

  public void deleteCode(String email) {
    redisTemplate.delete(List.of(CODE_KEY + email, ATTEMPTS_KEY + email));
  }

  public void saveVerifiedEmail(String token, String email, Duration ttl) {
    redisTemplate.opsForValue().set(TOKEN_KEY + token, email, ttl);
  }

  // GETDEL → 조회와 삭제를 한 번에 처리해 1회용 보장
  public Optional<String> consumeVerifiedEmail(String token) {
    return Optional.ofNullable(redisTemplate.opsForValue().getAndDelete(TOKEN_KEY + token));
  }
}
