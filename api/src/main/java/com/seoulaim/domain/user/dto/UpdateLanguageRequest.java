package com.seoulaim.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateLanguageRequest(
    @NotBlank @Pattern(regexp = "en|ko|zh|ja", message = "en, ko, zh, ja 중 하나여야 합니다")
        String language) {}
