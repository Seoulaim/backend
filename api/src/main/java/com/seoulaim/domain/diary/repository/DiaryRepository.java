package com.seoulaim.domain.diary.repository;

import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DiaryRepository extends JpaRepository<Diary, UUID> {

  // 삭제된 다이어리도 포함해 검사한다 (무료는 계정당 1회)
  boolean existsByUserIdAndIsFreeTrue(UUID userId);

  Optional<Diary> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

  /** 내 목록: 삭제되지 않았고 무료이거나 결제된 것만. */
  @Query(
      """
      select d from Diary d
      where d.userId = :userId and d.deletedAt is null and (d.isFree = true or d.paid = true)
      order by d.createdAt desc, d.id desc
      """)
  List<Diary> findVisibleByUserId(UUID userId);

  List<Diary> findAllByIdIn(Collection<UUID> ids);

  List<Diary> findAllByUserIdAndDeletedAtIsNull(UUID userId);

  List<Diary> findAllByUserIdOrderByCreatedAtDescIdDesc(UUID userId);

  List<Diary> findTop200ByDeletedAtIsNullOrderByCreatedAtDescIdDesc();

  @Query(
      """
      select d.userId as userId, count(d) as count from Diary d
      where d.userId in :userIds and d.deletedAt is null and (d.isFree = true or d.paid = true)
      group by d.userId
      """)
  List<UserCount> countVisibleByUserIds(Collection<UUID> userIds);

  /**
   * 대시보드 다이어리 수 (한 번의 스캔). "보이는" 다이어리 = 삭제 안 됐고 무료이거나 결제됨 (회원 앱 목록 규칙).
   *
   * <ul>
   *   <li>total: 보이는 다이어리
   *   <li>createdInRange: [from, to) 생성 + 무료이거나 결제됨 (이후 삭제된 것 포함)
   *   <li>finalized: 보이는 것 중 FINALIZED
   *   <li>finalizedInRange: [from, to) 완성 (이후 삭제된 것 포함)
   *   <li>paid: 보이는 것 중 결제됨 (환불되면 paid=false 라 빠진다)
   * </ul>
   */
  @Query(
      """
      select
        coalesce(sum(case when d.deletedAt is null and (d.isFree = true or d.paid = true)
          then 1 else 0 end), 0) as total,
        coalesce(sum(case when d.createdAt >= :from and d.createdAt < :to
          and (d.isFree = true or d.paid = true) then 1 else 0 end), 0) as createdInRange,
        coalesce(sum(case when d.deletedAt is null and (d.isFree = true or d.paid = true)
          and d.status = :finalized then 1 else 0 end), 0) as finalized,
        coalesce(sum(case when d.completedAt >= :from and d.completedAt < :to
          then 1 else 0 end), 0) as finalizedInRange,
        coalesce(sum(case when d.deletedAt is null and d.paid = true then 1 else 0 end), 0) as paid
      from Diary d
      """)
  DiaryCounts countForDashboard(Instant from, Instant to, DiaryStatus finalized);
}
