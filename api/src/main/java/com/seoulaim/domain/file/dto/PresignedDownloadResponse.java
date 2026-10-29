package com.seoulaim.domain.file.dto;

import java.time.Instant;

public record PresignedDownloadResponse(String downloadUrl, Instant expiresAt) {}
