package com.seoulaim.domain.content.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 홈(HH01)·다이어리(DD01) 배너 (어드민 "프로모션 관리")
 *
 * <ul>
 *   <li>기본 배너: 위치별 1개(처음엔 마이그레이션이 생성, 어드민이 다른 배너로 바꿀 수 있음). 항상 노출, 기간 없음, 끄기·삭제 불가
 *   <li>그 밖의 배너: enabled 이고 KST 오늘이 기간 안(종료일 포함)일 때 노출. 기간이 끝나도 enabled 는 그대로
 * </ul>
 */
@Getter
@Entity
@Table(name = "banners")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Banner extends BaseEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "placement", nullable = false, updatable = false, length = 10)
  private BannerPlacement placement;

  @Column(name = "admin_title", nullable = false, length = 100)
  private String adminTitle;

  @Column(name = "image_key", length = 512)
  private String imageKey;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "title", nullable = false, columnDefinition = "jsonb")
  private Map<String, String> title;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "description", nullable = false, columnDefinition = "jsonb")
  private Map<String, String> description;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "cta_label", nullable = false, columnDefinition = "jsonb")
  private Map<String, String> ctaLabel;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", nullable = false, length = 10)
  private BannerTargetType targetType;

  @Column(name = "target_url", length = 1000)
  private String targetUrl;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "enabled", nullable = false)
  private boolean enabled;

  @Column(name = "is_default", nullable = false)
  private boolean isDefault;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  /** 어드민이 추가하는 배너. 켜진 상태로 위치의 맨 뒤(displayOrder)에 붙는다. */
  public static Banner create(
      BannerPlacement placement,
      String adminTitle,
      String imageKey,
      Map<String, String> title,
      Map<String, String> description,
      Map<String, String> ctaLabel,
      BannerTargetType targetType,
      String targetUrl,
      LocalDate startDate,
      LocalDate endDate,
      int displayOrder) {
    Banner banner = new Banner();
    banner.placement = placement;
    banner.enabled = true;
    banner.isDefault = false;
    banner.displayOrder = displayOrder;
    banner.update(
        adminTitle,
        imageKey,
        title,
        description,
        ctaLabel,
        targetType,
        targetUrl,
        startDate,
        endDate);
    return banner;
  }

  /** placement 는 바꿀 수 없다. 기본 배너는 기간을 무시하고 null 로 둔다. */
  public void update(
      String adminTitle,
      String imageKey,
      Map<String, String> title,
      Map<String, String> description,
      Map<String, String> ctaLabel,
      BannerTargetType targetType,
      String targetUrl,
      LocalDate startDate,
      LocalDate endDate) {
    this.adminTitle = adminTitle;
    this.imageKey = imageKey;
    this.title = Map.copyOf(title);
    this.description = description == null ? Map.of() : Map.copyOf(description);
    this.ctaLabel = ctaLabel == null ? Map.of() : Map.copyOf(ctaLabel);
    this.targetType = targetType;
    this.targetUrl = targetUrl;
    this.startDate = isDefault ? null : startDate;
    this.endDate = isDefault ? null : endDate;
  }

  /**
   * 이 위치의 기본 배너로 지정한다. CHECK 제약(기본이면 enabled·기간 null)에 맞춰 켜고 기간을 지운다. 이전 기본 배너는 먼저 {@link
   * #unmarkDefault()} 후 flush 해야 한다 (위치별 partial unique index).
   */
  public void markDefault() {
    this.isDefault = true;
    this.enabled = true;
    this.startDate = null;
    this.endDate = null;
  }

  /** 기본 배너 해제. enabled·기간은 그대로 둔다 (기본 배너였으므로 켜짐·기간 없음 상태). */
  public void unmarkDefault() {
    this.isDefault = false;
  }

  /** 기본 배너는 호출하지 않는다 (서비스에서 CONTENT_DEFAULT_BANNER). */
  public void changeEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void changeDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }

  /** 회원 앱 노출 여부. today 는 KST 날짜. */
  public boolean isVisibleOn(LocalDate today) {
    if (isDefault) {
      return true;
    }
    return enabled
        && (startDate == null || !startDate.isAfter(today))
        && (endDate == null || !endDate.isBefore(today));
  }
}
