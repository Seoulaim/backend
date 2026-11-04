package com.seoulaim.domain.content.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 팬캘린더 일정 + Event coming up 카드 (어드민 "이벤트 관리" 의 calendar entry)
 *
 * <ul>
 *   <li>inCalendar → 팬캘린더에 startDate 로 표시 (사진은 안 보임)
 *   <li>imageKey 있음 → Event coming up 카드 (endDate 까지)
 *   <li>allGroups → 모든 회원 대상. 아니면 artistGroupIds 의 그룹 팬 대상
 * </ul>
 */
@Getter
@Entity
@Table(name = "calendar_entries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CalendarEntry extends BaseEntity {

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "title", nullable = false, columnDefinition = "jsonb")
  private Map<String, String> title;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "in_calendar", nullable = false)
  private boolean inCalendar;

  @Enumerated(EnumType.STRING)
  @Column(name = "color", length = 10)
  private CalendarColor color;

  @Column(name = "image_key", length = 512)
  private String imageKey;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "venue", columnDefinition = "jsonb")
  private Map<String, String> venue;

  @Column(name = "target_url", length = 1000)
  private String targetUrl;

  @Column(name = "all_groups", nullable = false)
  private boolean allGroups;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  // 목록 조회 시 N+1 방지 → 여러 항목의 그룹을 한 번에 로딩
  @BatchSize(size = 100)
  @ElementCollection
  @CollectionTable(name = "calendar_entry_groups", joinColumns = @JoinColumn(name = "entry_id"))
  @Column(name = "artist_group_id", nullable = false)
  private Set<UUID> artistGroupIds = new HashSet<>();

  public static CalendarEntry create(
      Map<String, String> title,
      LocalDate startDate,
      LocalDate endDate,
      boolean inCalendar,
      CalendarColor color,
      String imageKey,
      Map<String, String> venue,
      String targetUrl,
      boolean allGroups,
      Collection<UUID> artistGroupIds,
      int displayOrder) {
    CalendarEntry entry = new CalendarEntry();
    entry.displayOrder = displayOrder;
    entry.update(
        title,
        startDate,
        endDate,
        inCalendar,
        color,
        imageKey,
        venue,
        targetUrl,
        allGroups,
        artistGroupIds);
    return entry;
  }

  public void update(
      Map<String, String> title,
      LocalDate startDate,
      LocalDate endDate,
      boolean inCalendar,
      CalendarColor color,
      String imageKey,
      Map<String, String> venue,
      String targetUrl,
      boolean allGroups,
      Collection<UUID> artistGroupIds) {
    this.title = Map.copyOf(title);
    this.startDate = startDate;
    this.endDate = endDate;
    this.inCalendar = inCalendar;
    this.color = color;
    this.imageKey = imageKey;
    this.venue = venue == null ? null : Map.copyOf(venue);
    this.targetUrl = targetUrl;
    this.allGroups = allGroups;
    // 컬렉션 인스턴스는 유지 (Hibernate 가 변경분만 반영)
    this.artistGroupIds.clear();
    if (!allGroups) {
      this.artistGroupIds.addAll(artistGroupIds);
    }
  }
}
