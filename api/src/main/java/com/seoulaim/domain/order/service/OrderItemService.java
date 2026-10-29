package com.seoulaim.domain.order.service;

import com.seoulaim.domain.order.repository.OrderItemRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다른 도메인이 주문 상품 정보를 조회할 때 쓰는 서비스.
 *
 * <p>OrderService 는 ProductService 를 의존하므로, ProductService 가 OrderService 를 의존하면 순환이 된다. 그래서 따로 둔다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderItemService {

  private final OrderItemRepository orderItemRepository;

  public boolean existsByProductId(UUID productId) {
    return orderItemRepository.existsByProductId(productId);
  }
}
