package com.seoulaim.domain.inquiry.repository;

import com.seoulaim.domain.inquiry.entity.Inquiry;
import com.seoulaim.domain.inquiry.entity.InquiryCategory;
import com.seoulaim.domain.inquiry.entity.RefundStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {

  List<Inquiry> findAllByCategoryNotOrderByCreatedAtDescIdDesc(InquiryCategory category);

  Optional<Inquiry> findByIdAndCategoryNot(UUID id, InquiryCategory category);

  long countByRefundStatus(RefundStatus refundStatus);

  boolean existsByPaymentIdAndRefundStatusIn(UUID paymentId, Collection<RefundStatus> statuses);

  @Query(
      """
      select distinct i.paymentId from Inquiry i
      where i.paymentId in :paymentIds and i.refundStatus = :status
      """)
  List<UUID> findPaymentIdsByRefundStatus(Collection<UUID> paymentIds, RefundStatus status);
}
