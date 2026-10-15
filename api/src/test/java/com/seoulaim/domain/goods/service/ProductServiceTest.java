package com.seoulaim.domain.goods.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.goods.repository.ProductImageRepository;
import com.seoulaim.domain.goods.repository.ProductRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock private ProductRepository productRepository;
  @Mock private ProductImageRepository productImageRepository;
  @InjectMocks private ProductService productService;

  @Test
  @DisplayName("존재하지 않는 상품 ID 로 상세 조회하면 GOODS_NOT_FOUND 예외가 발생한다")
  void getById_존재하지않는ID_예외발생() {
    UUID productId = UUID.randomUUID();
    given(productRepository.findById(productId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> productService.getById(productId))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.GOODS_NOT_FOUND);
    verify(productImageRepository, never()).findAllByProductIdOrderByDisplayOrderAscIdAsc(any());
  }

  @Test
  @DisplayName("BEST 상품 size 가 최대값을 넘으면 20 개로 제한해 조회한다")
  void getBestProducts_size초과_최대20으로제한() {
    given(productRepository.findAllByOrderBySalesCount30dDescIdDesc(any())).willReturn(List.of());

    productService.getBestProducts(100);

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(productRepository).findAllByOrderBySalesCount30dDescIdDesc(pageableCaptor.capture());
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
  }
}
