package com.seoulaim.domain.diary.repository;

import com.seoulaim.domain.diary.entity.Diary;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryRepository extends JpaRepository<Diary, UUID> {

  boolean existsByOrderItemId(UUID orderItemId);

  List<Diary> findAllByOrderItemIdIn(Collection<UUID> orderItemIds);

  Optional<Diary> findByIdAndUserId(UUID id, UUID userId);

  List<Diary> findAllByUserIdOrderBySubmittedAtDescIdDesc(UUID userId);

  List<Diary> findTop200ByOrderBySubmittedAtDescIdDesc();
}
