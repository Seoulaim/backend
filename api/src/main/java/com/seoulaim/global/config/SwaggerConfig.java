package com.seoulaim.global.config;

import com.seoulaim.global.common.AdminKeyInterceptor;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  // 관리자 API 의 @SecurityRequirement 에서 참조한다 (Swagger UI Authorize 에 X-Admin-Key 입력)
  public static final String ADMIN_KEY_SCHEME = "AdminKey";

  @Bean
  public OpenAPI seoulaimOpenAPI() {
    return new OpenAPI()
        .info(new Info().title("Seoulaim API").description("Seoulaim Backend API").version("v1"))
        .components(
            new Components()
                .addSecuritySchemes(
                    ADMIN_KEY_SCHEME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(AdminKeyInterceptor.ADMIN_KEY_HEADER)));
  }
}
