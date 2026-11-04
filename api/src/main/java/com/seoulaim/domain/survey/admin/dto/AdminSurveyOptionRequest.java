package com.seoulaim.domain.survey.admin.dto;

import java.util.Map;
import java.util.UUID;

/** id 가 없으면 새 보기. label 은 { en, ko, zh, ja } (en 필수). */
public record AdminSurveyOptionRequest(UUID id, Map<String, String> label) {}
