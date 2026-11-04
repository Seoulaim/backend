package com.seoulaim.domain.survey.admin.controller;

import com.seoulaim.domain.survey.admin.dto.AdminSurveyAnswerResponse;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyEnabledRequest;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyEnabledResponse;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyGiftsSentRequest;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyResponse;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyResultsResponse;
import com.seoulaim.domain.survey.admin.dto.AdminSurveyUpdateRequest;
import com.seoulaim.domain.survey.service.AdminSurveyService;
import com.seoulaim.global.config.SwaggerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin")
@SecurityRequirement(name = SwaggerConfig.ADMIN_KEY_SCHEME)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/survey")
public class AdminSurveyController {

  private final AdminSurveyService adminSurveyService;

  @Operation(summary = "[관리자] 만족도 조사 설정 (숨긴 질문 포함, 질문별 응답 수)")
  @GetMapping
  public AdminSurveyResponse getSurvey() {
    return adminSurveyService.getSurvey();
  }

  @Operation(summary = "[관리자] 만족도 조사 문구·질문 전체 저장 (빠진 질문은 삭제, 응답 있으면 409)")
  @PutMapping
  public AdminSurveyResponse updateSurvey(@Valid @RequestBody AdminSurveyUpdateRequest request) {
    return adminSurveyService.updateSurvey(request);
  }

  @Operation(summary = "[관리자] 회원 앱 초대 팝업 켜기/끄기")
  @PatchMapping("/enabled")
  public AdminSurveyEnabledResponse setEnabled(
      @Valid @RequestBody AdminSurveyEnabledRequest request) {
    return adminSurveyService.setEnabled(request.enabled());
  }

  @Operation(summary = "[관리자] 응답 결과 (KST 날짜 범위, 최신순) + 팝업 누적 통계")
  @GetMapping("/responses")
  public AdminSurveyResultsResponse getResults(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return adminSurveyService.getResults(from, to);
  }

  @Operation(summary = "[관리자] 기프티콘 발송 완료 표시 (이미 발송된 응답은 건너뜀)")
  @PostMapping("/responses/gifts-sent")
  public List<AdminSurveyAnswerResponse> markGiftsSent(
      @Valid @RequestBody AdminSurveyGiftsSentRequest request) {
    return adminSurveyService.markGiftsSent(request.responseIds());
  }
}
