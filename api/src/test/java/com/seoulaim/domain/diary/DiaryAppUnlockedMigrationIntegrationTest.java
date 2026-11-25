package com.seoulaim.domain.diary;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** V20261126100000__diary_app_unlocked: 기존 다이어리가 app_unlocked=false 로 남아 결제 조건이 그대로인지 (#78). */
@Tag("integration")
@Testcontainers
class DiaryAppUnlockedMigrationIntegrationTest {

  private static final String BEFORE_APP_UNLOCKED = "20261119110000";

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
  @DisplayName("기존 다이어리는 app_unlocked=false 가 되고 is_free·paid 는 그대로다. 컬럼은 NOT NULL")
  void migrate_기존행_false() {
    flyway(BEFORE_APP_UNLOCKED).migrate();
    JdbcTemplate jdbc =
        new JdbcTemplate(
            new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
    UUID userId = UUID.randomUUID();
    UUID diaryId = UUID.randomUUID();
    Timestamp now = Timestamp.from(Instant.parse("2026-11-20T00:00:00Z"));
    jdbc.update(
        "insert into users (id, email, password_hash, nickname, created_at, updated_at)"
            + " values (?, 'old@example.com', 'hash', 'Old', ?, ?)",
        userId,
        now,
        now);
    jdbc.update(
        "insert into diaries (id, user_id, is_free, paid, status, created_at, updated_at)"
            + " values (?, ?, false, true, 'DRAFT', ?, ?)",
        diaryId,
        userId,
        now,
        now);

    flyway(null).migrate();

    Map<String, Object> row =
        jdbc.queryForMap("select app_unlocked, is_free, paid from diaries where id = ?", diaryId);
    assertThat(row)
        .containsEntry("app_unlocked", false)
        .containsEntry("is_free", false)
        .containsEntry("paid", true);
    assertThat(
            jdbc.queryForObject(
                "select is_nullable from information_schema.columns"
                    + " where table_name = 'diaries' and column_name = 'app_unlocked'",
                String.class))
        .isEqualTo("NO");
  }
}
