package com.seoulaim.domain.diary.repository;

import com.seoulaim.domain.diary.entity.DiaryPayment;
import com.seoulaim.domain.diary.entity.DiaryPaymentStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface DiaryPaymentRepository extends JpaRepository<DiaryPayment, UUID> {

  // 결제 승인 동시 요청이 게이트웨이를 두 번 호출하지 않도록 결제 행을 잠근다
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<DiaryPayment> findWithLockByIdAndUserId(UUID id, UUID userId);

  // 환불(결제 취소)이 게이트웨이를 두 번 호출하지 않도록 결제 행을 잠근다
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<DiaryPayment> findWithLockById(UUID id);

  Optional<DiaryPayment> findByIdAndUserId(UUID id, UUID userId);

  List<DiaryPayment> findAllByDiaryIdAndStatus(UUID diaryId, DiaryPaymentStatus status);

  List<DiaryPayment> findAllByDiaryIdInAndStatus(
      Collection<UUID> diaryIds, DiaryPaymentStatus status);

  Optional<DiaryPayment> findFirstByDiaryIdAndStatusOrderByPaidAtDescIdDesc(
      UUID diaryId, DiaryPaymentStatus status);

  List<DiaryPayment> findAllByUserIdAndStatusInOrderByPaidAtDescIdDesc(
      UUID userId, Collection<DiaryPaymentStatus> statuses);

  @Query(
      """
      select p.userId as userId, count(p) as count from DiaryPayment p
      where p.userId in :userIds and p.status = :status
      group by p.userId
      """)
  List<UserCount> countByUserIdsAndStatus(Collection<UUID> userIds, DiaryPaymentStatus status);
}
