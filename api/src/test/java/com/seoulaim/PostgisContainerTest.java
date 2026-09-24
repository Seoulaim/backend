package com.seoulaim;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Tag("integration")
class PostgisContainerTest {

  private static final DockerImageName POSTGIS_IMAGE =
      DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres");

  @Test
  void postgisContainerStarts() {
    try (PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(POSTGIS_IMAGE)
            .withDatabaseName("seoulaim")
            .withUsername("test")
            .withPassword("test")) {

      postgres.start();

      assertTrue(postgres.isRunning());
    }
  }
}
