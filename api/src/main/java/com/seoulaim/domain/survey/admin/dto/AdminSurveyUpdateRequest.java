package com.seoulaim.domain.survey.admin.dto;

import com.seoulaim.domain.survey.dto.SurveyInvite;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 설문 전체 저장. questions 순서가 곧 노출 순서. 빠진 기존 질문은 삭제 (응답이 있으면 409). */
public record AdminSurveyUpdateRequest(
    @NotNull @Valid SurveyInvite invite, @NotNull List<AdminSurveyQuestionRequest> questions) {}
