package com.seoulaim.domain.content.repository;

import com.seoulaim.domain.content.entity.BirthdayCafe;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BirthdayCafeRepository extends JpaRepository<BirthdayCafe, UUID> {

  List<BirthdayCafe> findAllByOrderByDisplayOrderAscIdAsc();

  // 끝나지 않은 생일카페 (endDate ≥ today), 어드민 순서
  List<BirthdayCafe> findAllByEndDateGreaterThanEqualOrderByDisplayOrderAscIdAsc(LocalDate today);

  @Query("SELECT COALESCE(MAX(c.displayOrder), -1) FROM BirthdayCafe c")
  int findMaxDisplayOrder();
}
