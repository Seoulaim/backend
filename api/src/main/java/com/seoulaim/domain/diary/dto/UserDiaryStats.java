package com.seoulaim.domain.diary.dto;

/**
 * 회원별 다이어리·결제 수 (어드민 화면용). API 응답으로 직접 쓰지 않는다.
 *
 * @param diaryCount 삭제되지 않았고 무료이거나 결제된 다이어리 수 (회원 앱 목록에 보이는 수)
 * @param paidPaymentCount 지금 PAID 인 결제 수 (환불된 결제 제외)
 */
public record UserDiaryStats(long diaryCount, long paidPaymentCount) {

  public static final UserDiaryStats EMPTY = new UserDiaryStats(0, 0);
}
