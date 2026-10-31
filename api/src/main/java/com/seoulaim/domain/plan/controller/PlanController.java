package com.seoulaim.domain.plan.controller;

import com.seoulaim.domain.plan.dto.CreatePlanRequest;
import com.seoulaim.domain.plan.dto.PlanDetailResponse;
import com.seoulaim.domain.plan.dto.PlanListRequest;
import com.seoulaim.domain.plan.dto.PlanSummaryResponse;
import com.seoulaim.domain.plan.dto.RecommendDatesRequest;
import com.seoulaim.domain.plan.dto.RecommendDatesResponse;
import com.seoulaim.domain.plan.service.PlanService;
import com.seoulaim.global.common.CurrentUserId;
import com.seoulaim.global.common.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {

  private final PlanService planService;

  @Operation(summary = "최적 투어 날짜 추천 (저장하지 않음)")
  @PostMapping("/recommended-dates")
  public RecommendDatesResponse recommendDates(
      @CurrentUserId UUID userId, @Valid @RequestBody RecommendDatesRequest request) {
    return planService.recommendDates(userId, request);
  }

  // @ResponseStatus → Swagger 에 201 표시 (실제 상태·Location 헤더는 ResponseEntity)
  @Operation(summary = "일정 생성 (추천 알고리즘)")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<PlanDetailResponse> createPlan(
      @CurrentUserId UUID userId, @Valid @RequestBody CreatePlanRequest request) {
    PlanDetailResponse response = planService.createPlan(userId, request);
    return ResponseEntity.created(URI.create("/api/v1/plans/" + response.id())).body(response);
  }

  @Operation(summary = "내 일정 목록 조회 (최신 생성순, size 기본 20·최대 50)")
  @GetMapping
  public CursorPageResponse<PlanSummaryResponse> getPlans(
      @CurrentUserId UUID userId, @ParameterObject @Valid @ModelAttribute PlanListRequest request) {
    return planService.findPlans(userId, request.cursor(), request.sizeOrDefault());
  }

  @Operation(summary = "일정 상세 조회")
  @GetMapping("/{planId}")
  public PlanDetailResponse getPlan(@CurrentUserId UUID userId, @PathVariable UUID planId) {
    return planService.getPlanDetail(userId, planId);
  }

  @Operation(summary = "일정 삭제")
  @DeleteMapping("/{planId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletePlan(@CurrentUserId UUID userId, @PathVariable UUID planId) {
    planService.deletePlan(userId, planId);
  }
}
