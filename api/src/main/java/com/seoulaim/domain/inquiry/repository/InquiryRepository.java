package com.seoulaim.domain.inquiry.repository;

import com.seoulaim.domain.inquiry.entity.Inquiry;
import com.seoulaim.domain.inquiry.entity.InquiryCategory;
import com.seoulaim.domain.inquiry.entity.RefundStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {

  List<Inquiry> findAllByCategoryNotOrderByCreatedAtDescIdDesc(InquiryCategory category);

  Optional<Inquiry> findByIdAndCategoryNot(UUID id, InquiryCategory category);

  List<Inquiry> findAllByCategoryOrderByCreatedAtAscIdAsc(InquiryCategory category);

  Optional<Inquiry> findByIdAndCategory(UUID id, InquiryCategory category);

  // 승인·거절 동시 요청이 토스 취소를 두 번 부르거나 결정을 덮어쓰지 않도록 문의 행을 잠근다
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Inquiry> findWithLockByIdAndCategory(UUID id, InquiryCategory category);

  long countByRefundStatus(RefundStatus refundStatus);

  boolean existsByPaymentIdAndRefundStatusIn(UUID paymentId, Collection<RefundStatus> statuses);

  @Query(
      """
      select distinct i.paymentId from Inquiry i
      where i.paymentId in :paymentIds and i.refundStatus = :status
      """)
  List<UUID> findPaymentIdsByRefundStatus(Collection<UUID> paymentIds, RefundStatus status);
}
