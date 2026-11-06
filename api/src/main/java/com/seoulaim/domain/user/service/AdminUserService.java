package com.seoulaim.domain.user.service;

import com.seoulaim.domain.artist.service.UserBiasService;
import com.seoulaim.domain.diary.dto.UserDiaryStats;
import com.seoulaim.domain.diary.service.DiaryPaymentService;
import com.seoulaim.domain.diary.service.DiaryService;
import com.seoulaim.domain.inquiry.dto.UserInquiryStats;
import com.seoulaim.domain.inquiry.service.RefundRequestQueryService;
import com.seoulaim.domain.user.admin.dto.AdminUserDetailResponse;
import com.seoulaim.domain.user.admin.dto.AdminUserListResponse;
import com.seoulaim.domain.user.dto.UserAccountInfo;
import com.seoulaim.domain.user.entity.UserStatus;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.util.AdminPaging;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 어드민 회원 관리: 목록·상세·강퇴.
 *
 * <p>다이어리·결제는 diary 서비스, 문의는 RefundRequestQueryService, 최애는 UserBiasService 경유. 강퇴는 회원 탈퇴와 같은
 * {@link UserWithdrawalService} 를 쓴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

  static final int MAX_WITHDRAW_REASON_LENGTH = 200;

  private final UserService userService;
  private final UserWithdrawalService userWithdrawalService;
  private final UserBiasService userBiasService;
  private final DiaryService diaryService;
  private final DiaryPaymentService diaryPaymentService;
  private final RefundRequestQueryService refundRequestQueryService;

  /**
   * @param keyword 이메일(마스킹 전)·닉네임 부분 일치, 대소문자 무시
   * @param status active | suspended | withdrawn (대소문자 무시). null·빈 값이면 전체
   */
  public AdminUserListResponse getUsers(String keyword, String status, int page, int size) {
    AdminPaging.validate(page, size);
    Page<UserAccountInfo> accounts =
        userService.searchAccounts(parseStatus(status), keyword, page, size);
    Map<UUID, UserDiaryStats> stats =
        diaryPaymentService.findUserStats(
            accounts.getContent().stream().map(UserAccountInfo::id).toList());
    List<AdminUserListResponse.Item> items =
        accounts.getContent().stream()
            .map(
                account -> {
                  UserDiaryStats stat = stats.getOrDefault(account.id(), UserDiaryStats.EMPTY);
                  return new AdminUserListResponse.Item(
                      account.id(),
                      account.nickname(),
                      EmailMasker.mask(account.email()),
                      account.locale(),
                      account.joinedAt(),
                      stat.diaryCount(),
                      stat.paidPaymentCount(),
                      lower(account.status()));
                })
            .toList();
    return new AdminUserListResponse(items, page, size, accounts.getTotalElements());
  }

  /** 없으면 USER_NOT_FOUND. 탈퇴 회원도 조회된다 (이메일 null). */
  public AdminUserDetailResponse getUser(UUID userId) {
    UserAccountInfo account = userService.getAccount(userId);
    UserInquiryStats inquiryStats =
        refundRequestQueryService
            .countUserStats(List.of(userId))
            .getOrDefault(userId, UserInquiryStats.EMPTY);
    return new AdminUserDetailResponse(
        account.id(),
        account.nickname(),
        account.email(),
        account.profileImageKey(),
        account.locale(),
        account.joinedAt(),
        lower(account.status()),
        account.withdrawnAt(),
        account.withdrawnBy() == null ? null : lower(account.withdrawnBy()),
        account.withdrawReason(),
        account.marketingEnabled(),
        account.marketingUpdatedAt(),
        account.pushEnabled(),
        userBiasService.findMyBiases(userId),
        diaryService.findDiariesOfUserForAdmin(userId).stream()
            .map(
                diary ->
                    new AdminUserDetailResponse.Diary(
                        diary.id(),
                        diary.name(),
                        diary.free(),
                        diary.paid(),
                        diary.finalized(),
                        diary.createdAt(),
                        diary.lastSavedAt(),
                        diary.saveCount(),
                        diary.finalizedAt(),
                        diary.downloadCount(),
                        diary.deleted()))
            .toList(),
        diaryPaymentService.findPaymentsOfUserForAdmin(userId).stream()
            .map(
                payment ->
                    new AdminUserDetailResponse.Payment(
                        payment.id(),
                        payment.orderNumber(),
                        payment.diaryId(),
                        payment.amount(),
                        payment.status(),
                        payment.paidAt(),
                        payment.refundedAt()))
            .toList(),
        refundRequestQueryService.findInquiriesOfUser(userId).stream()
            .map(
                inquiry ->
                    new AdminUserDetailResponse.Inquiry(
                        inquiry.id(),
                        inquiry.category(),
                        inquiry.title(),
                        inquiry.status(),
                        inquiry.createdAt(),
                        inquiry.refundStatus()))
            .toList(),
        new AdminUserDetailResponse.Stats(
            inquiryStats.refundedCount(),
            inquiryStats.rejectedRefundCount(),
            inquiryStats.pendingInquiryCount()));
  }

  /**
   * 강퇴: 회원 탈퇴와 같은 처리(최애·즐겨찾기·일정 삭제, 다이어리 soft delete, 결제 유지, refresh token 전부 폐기) + 사유 기록.
   *
   * <p>사유 1~200자 아니면 COMMON_INVALID_REQUEST, 없으면 USER_NOT_FOUND, 이미 탈퇴면 USER_ALREADY_WITHDRAWN.
   */
  @Transactional
  public void withdraw(UUID userId, String reason) {
    String trimmed = reason == null ? "" : reason.strip();
    if (trimmed.isEmpty()
        || trimmed.codePointCount(0, trimmed.length()) > MAX_WITHDRAW_REASON_LENGTH) {
      throw new BusinessException(
          ErrorCode.COMMON_INVALID_REQUEST,
          Map.of("reason", "1~" + MAX_WITHDRAW_REASON_LENGTH + "자여야 합니다"));
    }
    userWithdrawalService.withdrawByAdmin(userId, trimmed);
  }

  private static UserStatus parseStatus(String status) {
    if (status == null || status.isBlank()) {
      return null;
    }
    return Arrays.stream(UserStatus.values())
        .filter(value -> value.name().equalsIgnoreCase(status.strip()))
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessException(
                    ErrorCode.COMMON_INVALID_REQUEST,
                    Map.of("status", "active | suspended | withdrawn 중 하나여야 합니다")));
  }

  private static String lower(Enum<?> value) {
    return value.name().toLowerCase(Locale.ROOT);
  }
}
