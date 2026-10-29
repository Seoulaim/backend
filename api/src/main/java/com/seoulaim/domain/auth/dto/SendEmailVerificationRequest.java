package com.seoulaim.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendEmailVerificationRequest(@NotBlank @Email @Size(max = 255) String email) {}
