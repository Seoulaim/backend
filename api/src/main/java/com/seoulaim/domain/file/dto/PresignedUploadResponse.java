package com.seoulaim.domain.file.dto;

import java.time.Instant;
import java.util.UUID;

public record PresignedUploadResponse(
    UUID fileId, String s3Key, String uploadUrl, Instant expiresAt) {}
