package com.seoulaim.domain.goods.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seoulaim.domain.goods.service.ProductService;
import com.seoulaim.global.config.SecurityConfig;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminProductController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.admin.key=test-admin-key")
class AdminProductControllerTest {

  private static final String ADMIN_KEY = "test-admin-key";

  @Autowired private MockMvc mockMvc;
  @MockitoBean private ProductService productService;

  private String requestBody() {
    return """
        {
          "categoryId": "%s",
          "artistGroupId": "%s",
          "name": "Seoul Diary",
          "price": 25000,
          "isDiary": true,
          "imageFileIds": ["%s"]
        }
        """
        .formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
  }

  @Test
  @DisplayName("X-Admin-Key 헤더 없이 상품을 등록하면 403 AUTH_ACCESS_DENIED 를 반환한다")
  void createProduct_키없음_403반환() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/admin/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));

    verify(productService, never()).create(any());
  }

  @Test
  @DisplayName("X-Admin-Key 가 틀리면 403 AUTH_ACCESS_DENIED 를 반환한다")
  void createProduct_키불일치_403반환() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/admin/products")
                .header("X-Admin-Key", "wrong-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"));

    verify(productService, never()).create(any());
  }

  @Test
  @DisplayName("올바른 X-Admin-Key 와 요청이면 201 과 생성된 상품 id 를 반환한다")
  void createProduct_정상요청_201반환() throws Exception {
    UUID productId = UUID.randomUUID();
    given(productService.create(any())).willReturn(productId);

    mockMvc
        .perform(
            post("/api/v1/admin/products")
                .header("X-Admin-Key", ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(productId.toString()));
  }
}
