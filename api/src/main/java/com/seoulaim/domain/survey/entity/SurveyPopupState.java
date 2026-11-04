package com.seoulaim.domain.survey.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * 회원별 초대 팝업 상태 (PK = user_id).
 *
 * <p>동시 요청에서도 카운트가 빠지지 않도록 쓰기는 전부 SurveyPopupStateRepository 의 upsert 로 하고, 엔티티는 읽기 전용이다.
 */
@Getter
@Entity
@Immutable
@Table(name = "survey_popup_states")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyPopupState {

  @Id
  @Column(name = "user_id", columnDefinition = "uuid")
  private UUID userId;

  @Column(name = "travel_visits", nullable = false)
  private int travelVisits;

  @Column(name = "shown_count", nullable = false)
  private int shownCount;

  @Column(name = "closed_count", nullable = false)
  private int closedCount;

  @Column(name = "accepted_count", nullable = false)
  private int acceptedCount;

  @Column(name = "snoozed_count", nullable = false)
  private int snoozedCount;

  @Column(name = "snoozed_until")
  private Instant snoozedUntil;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public boolean isSnoozedAt(Instant now) {
    return snoozedUntil != null && snoozedUntil.isAfter(now);
  }
}
