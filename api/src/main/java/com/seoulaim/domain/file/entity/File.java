package com.seoulaim.domain.file.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "files")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class File extends BaseEntity {

  // TODO(#TBD): users 도메인 반영 후 FK 추가
  @Column(name = "uploader_id")
  private UUID uploaderId;

  @Enumerated(EnumType.STRING)
  @Column(name = "domain", nullable = false, length = 30)
  private FileDomain domain;

  @Column(name = "s3_key", nullable = false, unique = true, length = 512)
  private String s3Key;

  @Column(name = "content_type", length = 100)
  private String contentType;

  @Column(name = "size_bytes")
  private Long sizeBytes;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private FileStatus status;

  public static File createPending(
      UUID uploaderId, FileDomain domain, String s3Key, String contentType, Long sizeBytes) {
    File file = new File();
    file.uploaderId = uploaderId;
    file.domain = domain;
    file.s3Key = s3Key;
    file.contentType = contentType;
    file.sizeBytes = sizeBytes;
    file.status = FileStatus.PENDING;
    return file;
  }

  public void link() {
    this.status = FileStatus.LINKED;
  }

  public boolean isLinked() {
    return status == FileStatus.LINKED;
  }
}
