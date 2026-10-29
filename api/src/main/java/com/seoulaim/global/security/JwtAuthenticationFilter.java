package com.seoulaim.global.security;

import com.seoulaim.global.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization 헤더의 access token 검증 → SecurityContext 에 userId 등록
 *
 * <ul>
 *   <li>컨트롤러에서 {@code @AuthenticationPrincipal UUID userId} 로 사용
 *   <li>토큰 오류 시 바로 응답하지 않고 사유만 기록 → 공개 API 는 통과, 인증 필요 API 는 EntryPoint 가 401 응답
 *   <li>SecurityConfig 에서 직접 생성 (빈 등록 시 서블릿 필터로 중복 실행)
 * </ul>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  public static final String AUTH_ERROR_ATTRIBUTE = "authErrorCode";
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenProvider jwtTokenProvider;

  public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String header = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (header != null && header.startsWith(BEARER_PREFIX)) {
      String token = header.substring(BEARER_PREFIX.length());
      try {
        UUID userId = jwtTokenProvider.parseAccessToken(token);
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(userId, null, List.of()));
      } catch (BusinessException e) {
        request.setAttribute(AUTH_ERROR_ATTRIBUTE, e.getErrorCode());
      }
    }

    filterChain.doFilter(request, response);
  }
}
