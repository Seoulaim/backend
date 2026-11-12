package com.seoulaim.domain.diary.repository;

/** {@link DiaryRepository#countForDashboard} 결과 행. */
public interface DiaryCounts {

  long getTotal();

  long getCreatedInRange();

  long getFinalized();

  long getFinalizedInRange();

  long getPaid();
}
