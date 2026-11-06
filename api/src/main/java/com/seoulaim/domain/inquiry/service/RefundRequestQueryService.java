package com.seoulaim.domain.inquiry.service;

import com.seoulaim.domain.inquiry.dto.UserInquiryStats;
import com.seoulaim.domain.inquiry.entity.InquiryStatus;
import com.seoulaim.domain.inquiry.entity.RefundStatus;
import com.seoulaim.domain.inquiry.repository.InquiryRepository;
import com.seoulaim.domain.inquiry.repository.UserInquiryCount;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다른 도메인(diary 결제 내역, 어드민 결제·회원 관리)이 환불 요청 여부·문의 이력을 조회할 때 쓴다.
 *
 * <p>{@link InquiryService} 가 DiaryPaymentService 를 쓰므로, 순환 의존을 피하려고 저장소만 쓰는 조회 전용 서비스로 둔다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundRequestQueryService {

  private final InquiryRepository inquiryRepository;

  /** 처리 대기(REQUESTED) 환불 문의가 있는 결제 id. */
  public Set<UUID> findRequestedPaymentIds(Collection<UUID> paymentIds) {
    if (paymentIds.isEmpty()) {
      return Set.of();
    }
    return new HashSet<>(
        inquiryRepository.findPaymentIdsByRefundStatus(paymentIds, RefundStatus.REQUESTED));
  }

  /** 회원별 승인·거절 환불 수, 답변 전 문의 수. 문의가 없는 회원은 {@link UserInquiryStats#EMPTY}. */
  public Map<UUID, UserInquiryStats> countUserStats(Collection<UUID> userIds) {
    if (userIds.isEmpty()) {
      return Map.of();
    }
    List<UUID> ids = userIds.stream().distinct().toList();
    Map<UUID, long[]> counts = new HashMap<>();
    for (UserInquiryCount row : inquiryRepository.countByUserIds(ids)) {
      long[] count = counts.computeIfAbsent(row.getUserId(), id -> new long[3]);
      if (row.getRefundStatus() == RefundStatus.REFUNDED) {
        count[0] += row.getCount();
      } else if (row.getRefundStatus() == RefundStatus.REJECTED) {
        count[1] += row.getCount();
      }
      if (row.getStatus() == InquiryStatus.RECEIVED) {
        count[2] += row.getCount();
      }
    }
    return ids.stream()
        .collect(
            Collectors.toMap(
                Function.identity(),
                id -> {
                  long[] count = counts.get(id);
                  return count == null
                      ? UserInquiryStats.EMPTY
                      : new UserInquiryStats(count[0], count[1], count[2]);
                }));
  }
}
