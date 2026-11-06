package com.seoulaim.domain.diary.entity;

import com.seoulaim.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 완성 다이어리 다운로드 1회. 파일은 클라이언트가 만들고, 서버는 기록만 한다 (환불 정책 제4조 판단용).
 *
 * <p>다운로드 시각은 created_at.
 */
@Getter
@Entity
@Table(name = "diary_downloads")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryDownload extends BaseEntity {

  @Column(name = "diary_id", nullable = false)
  private UUID diaryId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "format", nullable = false, length = 10)
  private DiaryDownloadFormat format;

  @Column(name = "page_count", nullable = false)
  private int pageCount;

  public static DiaryDownload create(
      UUID diaryId, UUID userId, DiaryDownloadFormat format, int pageCount) {
    DiaryDownload download = new DiaryDownload();
    download.diaryId = diaryId;
    download.userId = userId;
    download.format = format;
    download.pageCount = pageCount;
    return download;
  }
}
