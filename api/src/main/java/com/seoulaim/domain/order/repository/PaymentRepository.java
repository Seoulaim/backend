package com.seoulaim.domain.order.repository;

import com.seoulaim.domain.order.entity.Payment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

  Optional<Payment> findByOrderId(UUID orderId);

  List<Payment> findAllByOrderIdIn(Collection<UUID> orderIds);
}
