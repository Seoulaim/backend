package com.seoulaim.domain.file.dto;

import com.seoulaim.domain.file.entity.File;
import com.seoulaim.domain.file.entity.FileStatus;
import java.util.UUID;

public record FileResponse(UUID fileId, String s3Key, FileStatus status, String contentType) {

  public static FileResponse from(File file) {
    return new FileResponse(file.getId(), file.getS3Key(), file.getStatus(), file.getContentType());
  }
}
