package com.seoulaim.domain.user.service;

import com.seoulaim.domain.artist.service.UserBiasService;
import com.seoulaim.domain.auth.service.AuthService;
import com.seoulaim.domain.diary.service.DiaryService;
import com.seoulaim.domain.place.service.PlaceFavoriteService;
import com.seoulaim.domain.plan.service.PlanService;
import com.seoulaim.domain.survey.service.SurveyService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴
 *
 * <ul>
 *   <li>UserService 에 두면 PlanService·UserBiasService·AuthService 와 순환 의존 → 별도 서비스
 *   <li>최애(bias)·장소 즐겨찾기·일정 삭제, 다이어리 soft delete, 결제 기록(diary_payments)은 유지
 *   <li>설문 응답은 남기고 email 만 "withdrawn" 으로 익명화 (응답 통계 유지)
 *   <li>users row 는 남기고 이메일·비밀번호만 지움 → 같은 이메일로 재가입 가능, 로그인은 AUTH_INVALID_CREDENTIALS
 *   <li>refresh token 은 DB 처리 후 마지막에 폐기 (Redis 실패 시 DB 롤백)
 *   <li>이미 탈퇴한 회원이면 아무것도 하지 않음 (멱등)
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserWithdrawalService {

  private final UserService userService;
  private final UserBiasService userBiasService;
  private final PlaceFavoriteService placeFavoriteService;
  private final PlanService planService;
  private final DiaryService diaryService;
  private final SurveyService surveyService;
  private final AuthService authService;

  @Transactional
  public void withdraw(UUID userId) {
    if (userService.getUser(userId).isWithdrawn()) {
      return;
    }
    // bias·즐겨찾기 벌크 delete 가 영속성 컨텍스트를 비움 → 가장 먼저 실행
    userBiasService.deleteAllBiasesOfUser(userId);
    placeFavoriteService.deleteAllFavoritesOfUser(userId);
    planService.deleteAllPlansOfUser(userId);
    diaryService.deleteAllDiariesOfUser(userId);
    surveyService.anonymizeResponseOfUser(userId);
    userService.withdraw(userId);
    authService.revokeAllRefreshTokens(userId);
  }
}
