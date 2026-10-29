package com.seoulaim.domain.diary.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "diaries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Diary extends BaseEntity {

  // TODO(#TBD): users 도메인 반영 후 FK 추가
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  // order 엔티티를 직접 참조하지 않으려고 id 만 보관한다. DB 에는 FK 가 있다
  @Column(name = "order_item_id", nullable = false, unique = true)
  private UUID orderItemId;

  @Column(name = "page_count", nullable = false)
  private int pageCount;

  // 웹앱 편집기 데이터. 서버는 구조를 검증하지 않고 그대로 보관·반환한다
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "content", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> content;

  @Column(name = "pdf_file_id", nullable = false)
  private UUID pdfFileId;

  @Column(name = "pdf_s3_key", nullable = false, length = 512)
  private String pdfS3Key;

  @Column(name = "submitted_at", nullable = false)
  private Instant submittedAt;

  public static Diary submit(
      UUID userId,
      UUID orderItemId,
      int pageCount,
      Map<String, Object> content,
      UUID pdfFileId,
      String pdfS3Key,
      Instant submittedAt) {
    Diary diary = new Diary();
    diary.userId = userId;
    diary.orderItemId = orderItemId;
    diary.pageCount = pageCount;
    diary.content = content;
    diary.pdfFileId = pdfFileId;
    diary.pdfS3Key = pdfS3Key;
    diary.submittedAt = submittedAt;
    return diary;
  }
}
