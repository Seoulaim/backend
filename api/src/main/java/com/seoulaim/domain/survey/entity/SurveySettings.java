package com.seoulaim.domain.survey.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 만족도 조사 설정. 테이블에 행 1개만 둔다 (uk_survey_settings_singleton).
 *
 * <p>enabled: 회원 앱 초대 팝업 노출 여부. invite: 팝업 제목·본문 (보상 안내 포함)
 */
@Getter
@Entity
@Table(name = "survey_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveySettings extends BaseEntity {

  @Column(name = "enabled", nullable = false)
  private boolean enabled;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "invite_title", nullable = false, columnDefinition = "jsonb")
  private Map<String, String> inviteTitle;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "invite_body", nullable = false, columnDefinition = "jsonb")
  private Map<String, String> inviteBody;

  public static SurveySettings createDefault() {
    SurveySettings settings = new SurveySettings();
    settings.enabled = false;
    settings.inviteTitle = Map.of();
    settings.inviteBody = Map.of();
    return settings;
  }

  public void changeEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void updateInvite(Map<String, String> title, Map<String, String> body) {
    this.inviteTitle = Map.copyOf(title);
    this.inviteBody = Map.copyOf(body);
  }
}
