package com.seoulaim.domain.inquiry.service;

import com.seoulaim.domain.inquiry.entity.RefundStatus;
import com.seoulaim.domain.inquiry.repository.InquiryRepository;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다른 도메인(diary 결제 내역)이 환불 요청 여부를 조회할 때 쓴다.
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
}
