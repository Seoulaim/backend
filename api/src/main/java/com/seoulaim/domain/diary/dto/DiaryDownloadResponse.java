package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.file.dto.PresignedDownloadResponse;
import java.time.Instant;

public record DiaryDownloadResponse(String downloadUrl, Instant expiresAt) {

  public static DiaryDownloadResponse from(PresignedDownloadResponse response) {
    return new DiaryDownloadResponse(response.downloadUrl(), response.expiresAt());
  }
}
