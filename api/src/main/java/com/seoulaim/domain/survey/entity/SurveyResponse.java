package com.seoulaim.domain.survey.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 회원의 설문 응답 (1인 1응답, user_id unique).
 *
 * <p>planSnapshot: 응답 시점 최신 플랜 요약. email: 응답 시점 계정 이메일 (기프티콘 받을 곳)
 */
@Getter
@Entity
@Table(name = "survey_responses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyResponse extends BaseEntity {

  // 다른 도메인 엔티티를 참조하지 않으려고 id 만 보관한다. DB 에는 users FK 가 있다
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "email", nullable = false, length = 255)
  private String email;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "plan_snapshot", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> planSnapshot;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "answers", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> answers;

  @Column(name = "submitted_at", nullable = false)
  private Instant submittedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "gift_status", nullable = false, length = 20)
  private SurveyGiftStatus giftStatus;

  @Column(name = "gift_sent_at")
  private Instant giftSentAt;

  public static final String WITHDRAWN_EMAIL = "withdrawn";

  public static SurveyResponse create(
      UUID userId,
      String email,
      Map<String, Object> planSnapshot,
      Map<String, Object> answers,
      Instant submittedAt) {
    SurveyResponse response = new SurveyResponse();
    response.userId = userId;
    response.email = email;
    response.planSnapshot = planSnapshot;
    response.answers = answers;
    response.submittedAt = submittedAt;
    response.giftStatus = SurveyGiftStatus.PENDING;
    return response;
  }

  // 회원 탈퇴 시 개인정보(이메일)만 제거. 응답·플랜 요약은 통계용으로 유지
  public void anonymizeEmail() {
    this.email = WITHDRAWN_EMAIL;
  }

  public boolean isGiftSent() {
    return giftStatus == SurveyGiftStatus.SENT;
  }

  public void markGiftSent(Instant now) {
    this.giftStatus = SurveyGiftStatus.SENT;
    this.giftSentAt = now;
  }
}
