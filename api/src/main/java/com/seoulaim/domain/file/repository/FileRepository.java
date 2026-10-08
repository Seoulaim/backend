package com.seoulaim.domain.file.repository;

import com.seoulaim.domain.file.entity.File;
import com.seoulaim.domain.file.entity.FileStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, UUID> {

  List<File> findAllByStatusAndCreatedAtBefore(FileStatus status, Instant createdAt);
}
