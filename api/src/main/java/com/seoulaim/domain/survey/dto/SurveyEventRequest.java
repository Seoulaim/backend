package com.seoulaim.domain.survey.dto;

import jakarta.validation.constraints.NotNull;

public record SurveyEventRequest(@NotNull SurveyEventType type) {}
