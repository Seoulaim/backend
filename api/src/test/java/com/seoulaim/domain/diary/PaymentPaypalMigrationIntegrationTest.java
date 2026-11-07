package com.seoulaim.domain.diary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** V20261107100000__payment_paypal: 기존 결제 행이 CARD/KRW 가 되고 금액·결제수단 상세가 유지되는지 (#59). */
@Tag("integration")
@Testcontainers
class PaymentPaypalMigrationIntegrationTest {

  private static final String BEFORE_PAYPAL = "20261106110000";

  @Container
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  private static Flyway flyway(String target) {
    var configuration =
        Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migration");
    if (target != null) {
      configuration.target(target);
    }
    return configuration.load();
  }

  @Test
  @DisplayName(
      "기존 행은 method=CARD·merchant=KRW·currency=KRW, amount 3000 은 3000.00, 기존 method 는 method_detail 로 옮겨진다")
  void migrate_기존행_CARD_KRW_금액유지() {
    flyway(BEFORE_PAYPAL).migrate();
    JdbcTemplate jdbc =
        new JdbcTemplate(
            new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
    UUID userId = UUID.randomUUID();
    UUID diaryId = UUID.randomUUID();
    UUID paymentId = UUID.randomUUID();
    Timestamp now = Timestamp.from(Instant.parse("2026-11-06T00:00:00Z"));
    jdbc.update(
        "insert into users (id, email, password_hash, nickname, created_at, updated_at)"
            + " values (?, 'old@example.com', 'hash', 'Old', ?, ?)",
        userId,
        now,
        now);
    jdbc.update(
        "insert into diaries (id, user_id, is_free, status, created_at, updated_at)"
            + " values (?, ?, false, 'DRAFT', ?, ?)",
        diaryId,
        userId,
        now,
        now);
    jdbc.update(
        "insert into diary_payments (id, diary_id, user_id, order_number, amount, status,"
            + " refund_consent_at, method, created_at, updated_at)"
            + " values (?, ?, ?, 'DIA-20261106-OLD0000001', 3000, 'PAID', ?, '간편결제', ?, ?)",
        paymentId,
        diaryId,
        userId,
        now,
        now,
        now);

    flyway(null).migrate();

    Map<String, Object> row =
        jdbc.queryForMap(
            "select method, merchant, currency, amount, method_detail from diary_payments"
                + " where id = ?",
            paymentId);
    assertThat(row)
        .containsEntry("method", "CARD")
        .containsEntry("merchant", "KRW")
        .containsEntry("currency", "KRW")
        .containsEntry("method_detail", "간편결제");
    assertThat((BigDecimal) row.get("amount")).isEqualByComparingTo("3000");
    assertThat(
            jdbc.queryForObject(
                "select numeric_precision || ',' || numeric_scale from information_schema.columns"
                    + " where table_name = 'diary_payments' and column_name = 'amount'",
                String.class))
        .isEqualTo("12,2");
    assertThatThrownBy(
            () -> jdbc.update("update diary_payments set currency = 'EUR' where id = ?", paymentId))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
