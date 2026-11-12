package com.seoulaim.domain.inquiry.repository;

import com.seoulaim.domain.inquiry.entity.Inquiry;
import com.seoulaim.domain.inquiry.entity.InquiryCategory;
import com.seoulaim.domain.inquiry.entity.InquiryStatus;
import com.seoulaim.domain.inquiry.entity.RefundStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
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

  List<Inquiry> findAllByUserIdOrderByCreatedAtDescIdDesc(UUID userId);

  List<Inquiry> findAllByPaymentIdInAndCategoryOrderByCreatedAtDescIdDesc(
      Collection<UUID> paymentIds, InquiryCategory category);

  boolean existsByPaymentIdAndRefundStatusIn(UUID paymentId, Collection<RefundStatus> statuses);

  @Query(
      """
      select distinct i.paymentId from Inquiry i
      where i.paymentId in :paymentIds and i.refundStatus = :status
      """)
  List<UUID> findPaymentIdsByRefundStatus(Collection<UUID> paymentIds, RefundStatus status);

  @Query(
      """
      select i.userId as userId, i.status as status, i.refundStatus as refundStatus,
        count(i) as count
      from Inquiry i
      where i.userId in :userIds
      group by i.userId, i.status, i.refundStatus
      """)
  List<UserInquiryCount> countByUserIds(Collection<UUID> userIds);

  // 대시보드 ---------------------------------------------------------------

  List<Inquiry> findAllByCategoryNotOrderByCreatedAtDescIdDesc(
      InquiryCategory category, Limit limit);

  List<Inquiry> findAllByCategoryOrderByCreatedAtDescIdDesc(InquiryCategory category, Limit limit);

  List<Inquiry> findAllByCategoryNotAndStatusOrderByCreatedAtDescIdDesc(
      InquiryCategory category, InquiryStatus status);

  List<Inquiry> findAllByCategoryAndRefundStatusOrderByCreatedAtDescIdDesc(
      InquiryCategory category, RefundStatus refundStatus);
}
