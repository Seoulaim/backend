package com.seoulaim.domain.diary.service;

import com.seoulaim.domain.diary.admin.dto.AdminDiarySummaryResponse;
import com.seoulaim.domain.diary.dto.DiaryCreateResponse;
import com.seoulaim.domain.diary.dto.DiaryNewTermsResponse;
import com.seoulaim.domain.diary.dto.DiarySummaryResponse;
import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.repository.DiaryRepository;
import com.seoulaim.global.config.DiaryProperties;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원 다이어리 생성·편집·완성. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

  static final int MAX_NAME_LENGTH = 30;

  private final DiaryRepository diaryRepository;
  private final DiaryProperties diaryProperties;
  private final Clock clock;

  /** 삭제되지 않았고 무료이거나 결제된 다이어리만 최신순. 미결제 유료 다이어리는 결제 전이라 숨긴다. */
  public List<DiarySummaryResponse> getMyDiaries(UUID userId) {
    return diaryRepository.findVisibleByUserId(userId).stream()
        .map(DiarySummaryResponse::from)
        .toList();
  }

  public DiaryNewTermsResponse getNewTerms(UUID userId) {
    return new DiaryNewTermsResponse(isNextFree(userId), diaryProperties.price());
  }

  /** 첫 다이어리(삭제한 것 포함)만 무료. 이후는 결제 전까지 저장할 수 없는 유료 다이어리로 만든다. */
  @Transactional
  public DiaryCreateResponse create(UUID userId) {
    Diary diary = Diary.create(userId, isNextFree(userId));
    try {
      // 무료 다이어리 동시 생성은 (user_id) WHERE is_free partial unique 인덱스로 막는다
      diaryRepository.saveAndFlush(diary);
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(
          ErrorCode.COMMON_INVALID_REQUEST, Map.of("diary", "동시에 생성 요청이 들어왔습니다. 다시 시도해주세요"));
    }
    return new DiaryCreateResponse(diary.getId());
  }

  @Transactional
  public void rename(UUID userId, UUID diaryId, String name) {
    String trimmed = name == null ? "" : name.strip();
    if (trimmed.isEmpty() || trimmed.codePointCount(0, trimmed.length()) > MAX_NAME_LENGTH) {
      throw new BusinessException(ErrorCode.DIARY_INVALID_NAME);
    }
    findMyDiary(userId, diaryId).rename(trimmed);
  }

  @Transactional
  public void delete(UUID userId, UUID diaryId) {
    findMyDiary(userId, diaryId).delete(clock.instant());
  }

  public List<AdminDiarySummaryResponse> getAdminDiaries() {
    return diaryRepository.findTop200ByDeletedAtIsNullOrderByCreatedAtDescIdDesc().stream()
        .map(AdminDiarySummaryResponse::from)
        .toList();
  }

  /** 삭제되지 않은 내 다이어리. 없거나 남의 것이면 DIARY_NOT_FOUND. */
  Diary findMyDiary(UUID userId, UUID diaryId) {
    return diaryRepository
        .findByIdAndUserIdAndDeletedAtIsNull(diaryId, userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));
  }

  private boolean isNextFree(UUID userId) {
    return !diaryRepository.existsByUserIdAndIsFreeTrue(userId);
  }
}
