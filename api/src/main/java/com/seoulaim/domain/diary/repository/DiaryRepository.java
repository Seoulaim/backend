package com.seoulaim.domain.diary.repository;

import com.seoulaim.domain.diary.entity.Diary;
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

  List<Diary> findTop200ByDeletedAtIsNullOrderByCreatedAtDescIdDesc();

  @Query(
      """
      select d.userId as userId, count(d) as count from Diary d
      where d.userId in :userIds and d.deletedAt is null and (d.isFree = true or d.paid = true)
      group by d.userId
      """)
  List<UserCount> countVisibleByUserIds(Collection<UUID> userIds);
}
