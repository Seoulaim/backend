package com.seoulaim.domain.survey.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

/** 초대 팝업 문구. 각각 { en, ko, zh, ja } (en 필수). 응답과 어드민 수정 요청에 함께 쓴다. */
public record SurveyInvite(@NotNull Map<String, String> title, @NotNull Map<String, String> body) {}
