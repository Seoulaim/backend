package com.seoulaim.domain.content.repository;

import com.seoulaim.domain.content.entity.CalendarEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CalendarEntryRepository extends JpaRepository<CalendarEntry, UUID> {

  // 어드민 순서. display_order 가 같으면 id(생성) 순
  List<CalendarEntry> findAllByOrderByDisplayOrderAscIdAsc();

  // Event coming up 후보: 사진 있고 끝나지 않은 것 (endDate ≥ today)
  List<CalendarEntry>
      findAllByImageKeyIsNotNullAndEndDateGreaterThanEqualOrderByDisplayOrderAscIdAsc(
          LocalDate today);

  // 팬캘린더 후보: inCalendar 이고 시작일이 기간 안 (from ~ to 포함), 날짜순
  List<CalendarEntry>
      findAllByInCalendarTrueAndStartDateBetweenOrderByStartDateAscDisplayOrderAscIdAsc(
          LocalDate from, LocalDate to);

  // 새 항목은 맨 뒤에 붙인다
  @Query("SELECT COALESCE(MAX(e.displayOrder), -1) FROM CalendarEntry e")
  int findMaxDisplayOrder();
}
