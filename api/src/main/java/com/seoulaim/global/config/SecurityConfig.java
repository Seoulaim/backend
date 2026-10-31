package com.seoulaim.global.config;

import com.seoulaim.global.security.JwtAccessDeniedHandler;
import com.seoulaim.global.security.JwtAuthenticationEntryPoint;
import com.seoulaim.global.security.JwtAuthenticationFilter;
import com.seoulaim.global.security.JwtTokenProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 보안 설정
 *
 * <ul>
 *   <li>세션 미사용, JWT access token 으로 인증
 *   <li>공개 경로 외 전부 인증 필요
 *   <li>401·403 은 ErrorResponse 형식으로 응답
 *   <li>웹 클라이언트용 CORS 허용 (origin 은 app.cors.allowed-origins)
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

  private static final String LOGOUT_PATH = "/api/v1/auth/logout";

  // /error: 처리되지 않은 예외의 Spring 기본 에러 응답 경로. 막으면 500 대신 401 로 보임
  private static final String[] PUBLIC_PATHS = {
    "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health", "/error", "/api/v1/auth/**"
  };

  // 비로그인 조회 허용 (상품·카테고리·프로모션)
  private static final String[] PUBLIC_GET_PATHS = {
    "/api/v1/products/**", "/api/v1/product-categories/**", "/api/v1/promotions/**"
  };

  // 관리자 API 는 JWT 대신 X-Admin-Key 로 검증 (AdminKeyInterceptor)
  private static final String ADMIN_PATH = "/api/v1/admin/**";

  private final JwtTokenProvider jwtTokenProvider;
  private final JwtAuthenticationEntryPoint authenticationEntryPoint;
  private final JwtAccessDeniedHandler accessDeniedHandler;

  @Value("${app.cors.allowed-origins}")
  private List<String> allowedOrigins;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth
                    // CORS preflight 는 인증 없이 통과
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    // auth 경로 중 로그아웃만 인증 필요 → permitAll 보다 먼저 선언
                    .requestMatchers(HttpMethod.POST, LOGOUT_PATH)
                    .authenticated()
                    .requestMatchers(PUBLIC_PATHS)
                    .permitAll()
                    .requestMatchers(ADMIN_PATH)
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, PUBLIC_GET_PATHS)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exception ->
                exception
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtTokenProvider),
            UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  // Bearer 헤더 인증 → 쿠키 미사용, allowCredentials 불필요
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(allowedOrigins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Admin-Key"));
    // 웹에서 읽어야 하는 응답 헤더 (일정 생성 Location, 오류 추적 X-Trace-Id)
    configuration.setExposedHeaders(List.of("Location", "X-Trace-Id"));
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
