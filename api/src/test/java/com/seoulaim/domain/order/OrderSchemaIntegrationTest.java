package com.seoulaim.domain.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.seoulaim.domain.goods.entity.Product;
import com.seoulaim.domain.goods.entity.ProductCategory;
import com.seoulaim.domain.goods.repository.ProductCategoryRepository;
import com.seoulaim.domain.goods.repository.ProductRepository;
import com.seoulaim.domain.order.entity.ContactType;
import com.seoulaim.domain.order.entity.Order;
import com.seoulaim.domain.order.entity.OrderItem;
import com.seoulaim.domain.order.entity.OrderStatus;
import com.seoulaim.domain.order.entity.Payment;
import com.seoulaim.domain.order.entity.PaymentStatus;
import com.seoulaim.domain.order.entity.PgProvider;
import com.seoulaim.domain.order.repository.OrderItemRepository;
import com.seoulaim.domain.order.repository.OrderRepository;
import com.seoulaim.domain.order.repository.PaymentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
@SpringBootTest
@Testcontainers
class OrderSchemaIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @Autowired private ProductCategoryRepository productCategoryRepository;
  @Autowired private ProductRepository productRepository;
  @Autowired private OrderRepository orderRepository;
  @Autowired private OrderItemRepository orderItemRepository;
  @Autowired private PaymentRepository paymentRepository;

  @Test
  @DisplayName("order 스키마가 적용된 상태에서 주문·주문상품·결제를 저장하고 jsonb 응답까지 조회된다")
  void save_whenSchemaMigrated_findReturnsSavedOrder() {
    ProductCategory category = productCategoryRepository.save(ProductCategory.create("Doll", 0));
    Product product =
        productRepository.save(
            Product.create(category, UUID.randomUUID(), null, "Doll", null, null, 15000L, false));
    UUID userId = UUID.randomUUID();
    Order order =
        orderRepository.save(
            Order.createPending(
                "ORD-20261029-AB12CD",
                userId,
                30000L,
                3000L,
                "Kim",
                "서울특별시 중구 세종대로 110",
                null,
                "04524",
                ContactType.KAKAO,
                "seoul_fan",
                Instant.parse("2026-10-29T03:00:00Z")));
    orderItemRepository.save(OrderItem.create(order.getId(), product.getId(), "Doll", 15000L, 2));
    Payment payment =
        Payment.createReady(order.getId(), PgProvider.TOSS, order.getOrderNumber(), 33000L);
    payment.markDone(
        "tgen_abc",
        "카드",
        Instant.parse("2026-10-29T03:01:00Z"),
        "{\"status\": \"DONE\", \"totalAmount\": 33000}");
    paymentRepository.save(payment);

    Order found = orderRepository.findByIdAndUserId(order.getId(), userId).orElseThrow();
    List<OrderItem> items =
        orderItemRepository.findAllByOrderIdInOrderByIdAsc(List.of(order.getId()));
    Payment foundPayment = paymentRepository.findByOrderId(order.getId()).orElseThrow();

    assertThat(found.getStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(found.getFinalAmount()).isEqualTo(33000L);
    assertThat(found.getContactType()).isEqualTo(ContactType.KAKAO);
    assertThat(found.getId().version()).isEqualTo(7);
    assertThat(items).extracting(OrderItem::getQuantity).containsExactly(2);
    assertThat(orderItemRepository.existsByProductId(product.getId())).isTrue();
    assertThat(foundPayment.getStatus()).isEqualTo(PaymentStatus.DONE);
    assertThat(foundPayment.getRawResponse()).contains("\"totalAmount\"");
    assertThat(orderRepository.findTop50ByUserIdOrderByOrderedAtDescIdDesc(userId)).hasSize(1);
  }
}
