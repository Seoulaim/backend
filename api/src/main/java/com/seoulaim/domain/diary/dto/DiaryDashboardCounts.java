package com.seoulaim.domain.diary.dto;

/**
 * 어드민 대시보드 다이어리 수. "보이는" 다이어리 = 삭제 안 됐고 무료이거나 결제됨 (회원 앱 목록 규칙).
 *
 * @param total 보이는 다이어리
 * @param createdInRange 기간 내 생성 + 무료이거나 결제됨 (이후 삭제된 것 포함, 결제 안 한 빈 다이어리 제외)
 * @param finalized 보이는 것 중 완성(FINALIZED)
 * @param finalizedInRange 기간 내 완성 (이후 삭제된 것 포함)
 * @param paid 보이는 것 중 결제된 다이어리 (환불되면 빠진다)
 */
public record DiaryDashboardCounts(
    long total, long createdInRange, long finalized, long finalizedInRange, long paid) {}
