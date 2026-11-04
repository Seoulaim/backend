package com.seoulaim.domain.survey.admin.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record AdminSurveyGiftsSentRequest(@NotNull List<UUID> responseIds) {}
