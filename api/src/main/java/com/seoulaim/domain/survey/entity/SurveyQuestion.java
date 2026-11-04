package com.seoulaim.domain.survey.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 설문 질문. id 는 바뀌지 않아 수정 후에도 응답과 계속 맞물린다.
 *
 * <p>응답이 있는 질문은 삭제하지 않고 archived 로 숨긴다 (회원 앱에서 제외, 결과 화면에는 표시).
 */
@Getter
@Entity
@Table(name = "survey_questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyQuestion extends BaseEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 20)
  private SurveyQuestionType type;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "title", nullable = false, columnDefinition = "jsonb")
  private Map<String, String> title;

  @Column(name = "required", nullable = false)
  private boolean required;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(name = "archived", nullable = false)
  private boolean archived;

  public static SurveyQuestion create(
      SurveyQuestionType type,
      Map<String, String> title,
      boolean required,
      boolean archived,
      int displayOrder) {
    SurveyQuestion question = new SurveyQuestion();
    question.update(type, title, required, archived, displayOrder);
    return question;
  }

  public void update(
      SurveyQuestionType type,
      Map<String, String> title,
      boolean required,
      boolean archived,
      int displayOrder) {
    this.type = type;
    this.title = Map.copyOf(title);
    this.required = required;
    this.archived = archived;
    this.displayOrder = displayOrder;
  }
}
