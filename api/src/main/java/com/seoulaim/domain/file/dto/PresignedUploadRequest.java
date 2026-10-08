package com.seoulaim.domain.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// domain 은 String 으로 받아 서비스에서 FILE_INVALID_DOMAIN 으로 검증한다 (enum 역직렬화 실패는 500 이 된다)
public record PresignedUploadRequest(
    @NotBlank String domain, @NotBlank String contentType, @NotNull @Positive Long sizeBytes) {}
