package com.seoulaim.domain.inquiry.service;

import com.seoulaim.domain.diary.dto.UserDiaryStats;
import com.seoulaim.domain.diary.service.DiaryPaymentService;
import com.seoulaim.domain.inquiry.admin.dto.RefundAuthor;
import com.seoulaim.domain.inquiry.dto.UserInquiryStats;
import com.seoulaim.domain.inquiry.entity.Inquiry;
import com.seoulaim.domain.user.dto.UserAccountInfo;
import com.seoulaim.domain.user.service.UserService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 환불 요청 작성자와 계정 이력(가입일·다이어리·결제·환불·문의 수)을 회원 단위로 한 번에 불러온다. */
@Component
@RequiredArgsConstructor
class RefundAuthorLoader {

  private final UserService userService;
  private final DiaryPaymentService diaryPaymentService;
  private final RefundRequestQueryService refundRequestQueryService;

  Map<UUID, RefundAuthor> load(Collection<Inquiry> inquiries) {
    List<UUID> userIds = inquiries.stream().map(Inquiry::getUserId).distinct().toList();
    Map<UUID, UserAccountInfo> accounts =
        userService.findAccounts(userIds).stream()
            .collect(Collectors.toMap(UserAccountInfo::id, Function.identity()));
    Map<UUID, UserDiaryStats> diaryStats = diaryPaymentService.findUserStats(userIds);
    Map<UUID, UserInquiryStats> inquiryStats = refundRequestQueryService.countUserStats(userIds);
    return userIds.stream()
        .collect(
            Collectors.toMap(
                Function.identity(),
                userId ->
                    RefundAuthor.of(
                        userId,
                        accounts.get(userId),
                        diaryStats.getOrDefault(userId, UserDiaryStats.EMPTY),
                        inquiryStats.getOrDefault(userId, UserInquiryStats.EMPTY))));
  }

  RefundAuthor load(Inquiry inquiry) {
    return load(List.of(inquiry)).get(inquiry.getUserId());
  }
}
