package com.seoulaim.global.config;

import com.seoulaim.global.common.AdminKeyInterceptor;
import com.seoulaim.global.common.CurrentUserIdArgumentResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// AdminKeyInterceptor 가 쓰는 AdminProperties 를 @WebMvcTest 슬라이스에서도 등록되게 여기서 활성화한다
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

  // JWT 인증 정보에서 유저 ID 를 꺼내는 구현 (JwtCurrentUserIdArgumentResolver)
  private final CurrentUserIdArgumentResolver currentUserIdArgumentResolver;
  private final AdminKeyInterceptor adminKeyInterceptor;

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(currentUserIdArgumentResolver);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(adminKeyInterceptor).addPathPatterns("/api/v1/admin/**");
  }
}
