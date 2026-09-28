package com.seoulaim.domain.goods.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.seoulaim.domain.goods.dto.ProductDetailResponse;
import com.seoulaim.domain.goods.service.ProductService;
import com.seoulaim.global.config.SecurityConfig;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private ProductService productService;

  @Test
  @DisplayName("상품 상세 조회에 성공하면 200 과 상세 정보를 반환한다")
  void getProduct_존재하는ID_200반환() throws Exception {
    UUID productId = UUID.randomUUID();
    UUID categoryId = UUID.randomUUID();
    ProductDetailResponse response =
        new ProductDetailResponse(
            productId,
            "Seoul Diary",
            "summary",
            25000L,
            categoryId,
            "Diary",
            UUID.randomUUID(),
            null,
            true,
            List.of("goods/1.jpg", "goods/2.jpg"),
            "goods/detail.jpg");
    given(productService.getById(productId)).willReturn(response);

    mockMvc
        .perform(get("/api/v1/products/{productId}", productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(productId.toString()))
        .andExpect(jsonPath("$.categoryName").value("Diary"))
        .andExpect(jsonPath("$.isDiary").value(true))
        .andExpect(jsonPath("$.imageKeys.length()").value(2));
  }

  @Test
  @DisplayName("존재하지 않는 상품을 상세 조회하면 404 와 GOODS_NOT_FOUND 를 반환한다")
  void getProduct_존재하지않는ID_404반환() throws Exception {
    UUID productId = UUID.randomUUID();
    given(productService.getById(productId))
        .willThrow(new BusinessException(ErrorCode.GOODS_NOT_FOUND));

    mockMvc
        .perform(get("/api/v1/products/{productId}", productId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("GOODS_NOT_FOUND"));
  }

  @Test
  @DisplayName("상품 목록 조회에 잘못된 cursor 를 넘기면 400 과 GOODS_INVALID_CURSOR 를 반환한다")
  void getProducts_잘못된cursor_400반환() throws Exception {
    mockMvc
        .perform(get("/api/v1/products").param("cursor", "not-a-valid-cursor"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("GOODS_INVALID_CURSOR"));

    verify(productService, never()).getProducts(any());
  }
}
