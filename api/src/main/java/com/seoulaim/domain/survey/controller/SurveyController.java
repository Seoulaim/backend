package com.seoulaim.domain.survey.controller;

import com.seoulaim.domain.survey.dto.PlanSurveyResponse;
import com.seoulaim.domain.survey.dto.PlanSurveySubmitRequest;
import com.seoulaim.domain.survey.dto.SurveyEventRequest;
import com.seoulaim.domain.survey.dto.TravelVisitResponse;
import com.seoulaim.domain.survey.service.SurveyService;
import com.seoulaim.global.common.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Survey")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/me")
public class SurveyController {

  private final SurveyService surveyService;

  @Operation(summary = "TRAVEL 방문 기록 후 지금 띄울 만족도 조사 초대 (없으면 surveyInvite null)")
  @PostMapping("/travel-visits")
  public TravelVisitResponse recordTravelVisit(
      @Parameter(hidden = true) @CurrentUserId UUID userId) {
    return surveyService.recordTravelVisit(userId);
  }

  @Operation(summary = "초대 팝업 이벤트 기록 (SHOWN / ACCEPTED / CLOSED / SNOOZED)")
  @PostMapping("/plan-survey/events")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void recordEvent(
      @Parameter(hidden = true) @CurrentUserId UUID userId,
      @Valid @RequestBody SurveyEventRequest request) {
    surveyService.recordEvent(userId, request.type());
  }

  @Operation(summary = "설문 화면 (숨기지 않은 질문, 계정 이메일, 응답 여부). 플랜이 없으면 404")
  @GetMapping("/plan-survey")
  public PlanSurveyResponse getPlanSurvey(@Parameter(hidden = true) @CurrentUserId UUID userId) {
    return surveyService.getPlanSurvey(userId);
  }

  @Operation(summary = "설문 응답 제출 (1인 1회)")
  @PostMapping("/plan-survey")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void submit(
      @Parameter(hidden = true) @CurrentUserId UUID userId,
      @Valid @RequestBody PlanSurveySubmitRequest request) {
    surveyService.submit(userId, request.answers());
  }
}
