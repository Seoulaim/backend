package com.seoulaim.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// newPassword 규칙은 서비스에서 검증 → 위반 시 AUTH_INVALID_PASSWORD (COMMON_INVALID_REQUEST 아님)
public record ResetPasswordRequest(
    @NotBlank @Email @Size(max = 255) String email, @NotBlank String code, String newPassword) {}
