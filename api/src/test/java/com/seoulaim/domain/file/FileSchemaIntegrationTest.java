package com.seoulaim.domain.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.seoulaim.domain.file.entity.File;
import com.seoulaim.domain.file.entity.FileDomain;
import com.seoulaim.domain.file.entity.FileStatus;
import com.seoulaim.domain.file.repository.FileRepository;
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
class FileSchemaIntegrationTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres =
      new PostgreSQLContainer(
          DockerImageName.parse("postgis/postgis:16-3.4").asCompatibleSubstituteFor("postgres"));

  @Autowired private FileRepository fileRepository;

  @Test
  @DisplayName("files 스키마가 적용된 상태에서 파일을 저장하면 PENDING 상태로 조회된다")
  void save_whenSchemaMigrated_findByIdReturnsSavedFile() {
    UUID uploaderId = UUID.randomUUID();
    File saved =
        fileRepository.save(
            File.createPending(
                uploaderId, FileDomain.DIARY, "diary/2026/10/test.jpg", "image/jpeg", 1024L));

    File found = fileRepository.findById(saved.getId()).orElseThrow();

    assertThat(found.getUploaderId()).isEqualTo(uploaderId);
    assertThat(found.getDomain()).isEqualTo(FileDomain.DIARY);
    assertThat(found.getS3Key()).isEqualTo("diary/2026/10/test.jpg");
    assertThat(found.getSizeBytes()).isEqualTo(1024L);
    assertThat(found.getStatus()).isEqualTo(FileStatus.PENDING);
    assertThat(found.getId().version()).isEqualTo(7);
    assertThat(found.getCreatedAt()).isNotNull();
  }
}
