package com.seoulaim.domain.diary.repository;

import com.seoulaim.domain.diary.entity.DiaryPayment;
import com.seoulaim.domain.diary.entity.DiaryPaymentStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
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

  // 대시보드 ---------------------------------------------------------------

  /** 최근 생성된 결제 (상태 무관, 어드민 결제 목록 기본 정렬과 같음). */
  List<DiaryPayment> findAllByOrderByCreatedAtDescIdDesc(Limit limit);

  /** [from, to) 에 승인된 결제를 KST 날짜·통화·수단별로 합산. 나중에 환불된 결제도 승인 시점에 센다 (환불은 따로 집계). */
  @Query(
      value =
          """
          SELECT to_char(p.paid_at AT TIME ZONE 'Asia/Seoul', 'YYYY-MM-DD') AS day,
                 p.currency AS currency, p.method AS method,
                 COUNT(*) AS count, SUM(p.amount) AS amount
          FROM diary_payments p
          WHERE p.status IN ('PAID', 'REFUNDED') AND p.paid_at >= :from AND p.paid_at < :to
          GROUP BY day, p.currency, p.method
          """,
      nativeQuery = true)
  List<PaymentDailyTotal> sumPaidByKstDate(Instant from, Instant to);

  /** [from, to) 에 환불된 결제를 KST 날짜·통화·수단별로 합산. */
  @Query(
      value =
          """
          SELECT to_char(p.refunded_at AT TIME ZONE 'Asia/Seoul', 'YYYY-MM-DD') AS day,
                 p.currency AS currency, p.method AS method,
                 COUNT(*) AS count, SUM(p.amount) AS amount
          FROM diary_payments p
          WHERE p.status = 'REFUNDED' AND p.refunded_at >= :from AND p.refunded_at < :to
          GROUP BY day, p.currency, p.method
          """,
      nativeQuery = true)
  List<PaymentDailyTotal> sumRefundedByKstDate(Instant from, Instant to);
}
