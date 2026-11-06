package com.seoulaim.domain.user.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 어드민 회원 목록 한 페이지 (가입 최신순). */
public record AdminUserListResponse(List<Item> items, int page, int size, long totalElements) {

  /**
   * @param emailMasked "ab***@gmail.com". 탈퇴 회원은 null
   * @param language 앱 언어 (en | ko | ja | zh)
   * @param diaryCount 삭제되지 않았고 무료이거나 결제된 다이어리 수
   * @param paidPaymentCount 지금 paid 인 결제 수 (환불 제외)
   * @param status 소문자 (active | suspended | withdrawn)
   */
  public record Item(
      UUID id,
      String nickname,
      String emailMasked,
      String language,
      Instant joinedAt,
      long diaryCount,
      long paidPaymentCount,
      String status) {}
}
