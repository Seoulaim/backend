package com.seoulaim.domain.survey.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** SINGLE / MULTIPLE 질문의 보기. 응답에는 보기 id 가 저장된다. */
@Getter
@Entity
@Table(name = "survey_options")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyOption extends BaseEntity {

  // DB 에는 survey_questions FK 가 있다. 질문 단위로 묶어 조회·삭제하므로 id 만 보관
  @Column(name = "question_id", nullable = false)
  private UUID questionId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "label", nullable = false, columnDefinition = "jsonb")
  private Map<String, String> label;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  public static SurveyOption create(UUID questionId, Map<String, String> label, int displayOrder) {
    SurveyOption option = new SurveyOption();
    option.questionId = questionId;
    option.update(label, displayOrder);
    return option;
  }

  public void update(Map<String, String> label, int displayOrder) {
    this.label = Map.copyOf(label);
    this.displayOrder = displayOrder;
  }
}
