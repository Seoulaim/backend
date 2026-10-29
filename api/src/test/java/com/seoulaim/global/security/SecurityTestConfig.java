package com.seoulaim.global.security;

import com.seoulaim.global.config.ClockConfig;
import com.seoulaim.global.config.SecurityConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * {@code @WebMvcTest} 용 보안 설정 묶음
 *
 * <ul>
 *   <li>슬라이스 테스트는 일반 @Component 를 띄우지 않음 → SecurityConfig 의존 빈을 직접 등록
 *   <li>실제 JWT 필터 체인 그대로 사용
 * </ul>
 */
@TestConfiguration
@Import({
  SecurityConfig.class,
  ClockConfig.class,
  JwtTokenProvider.class,
  JwtAuthenticationEntryPoint.class,
  JwtAccessDeniedHandler.class,
  SecurityErrorResponseWriter.class,
  TestAccessTokens.class
})
public class SecurityTestConfig {}
