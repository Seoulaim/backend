package com.seoulaim.domain.file.dto;

import com.seoulaim.domain.file.entity.File;
import com.seoulaim.domain.file.entity.FileDomain;
import com.seoulaim.domain.file.entity.FileStatus;
import java.util.UUID;

/** 다른 도메인이 파일을 연결하기 전에 검증용으로 조회하는 정보. API 응답으로 쓰지 않는다. */
public record FileDetail(
    UUID fileId,
    UUID uploaderId,
    FileDomain domain,
    String s3Key,
    String contentType,
    FileStatus status) {

  public static FileDetail from(File file) {
    return new FileDetail(
        file.getId(),
        file.getUploaderId(),
        file.getDomain(),
        file.getS3Key(),
        file.getContentType(),
        file.getStatus());
  }
}
