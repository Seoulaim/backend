package com.seoulaim.domain.order.repository;

import com.seoulaim.domain.order.entity.Order;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface OrderRepository extends JpaRepository<Order, UUID> {

  Optional<Order> findByIdAndUserId(UUID id, UUID userId);

  // 결제 승인 동시 요청이 게이트웨이를 두 번 호출하지 않도록 주문 행을 잠근다
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Order> findWithLockByIdAndUserId(UUID id, UUID userId);

  List<Order> findTop50ByUserIdOrderByOrderedAtDescIdDesc(UUID userId);
}
