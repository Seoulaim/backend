package com.seoulaim.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$",
            message = "8~64자, 영문·숫자를 각 1자 이상 포함해야 합니다")
        String password,
    @NotBlank @Size(max = 20) String nickname,
    @NotBlank String verificationToken) {}
