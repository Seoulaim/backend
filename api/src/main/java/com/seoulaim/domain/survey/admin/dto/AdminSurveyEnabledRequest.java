package com.seoulaim.domain.survey.admin.dto;

import jakarta.validation.constraints.NotNull;

public record AdminSurveyEnabledRequest(@NotNull Boolean enabled) {}
