package com.seoulaim.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.diary.dto.DiaryCreateResponse;
import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.repository.DiaryRepository;
import com.seoulaim.global.config.DiaryProperties;
import com.seoulaim.global.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiaryServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-03T03:00:00Z");
  private static final UUID USER_ID = UUID.randomUUID();

  @Mock private DiaryRepository diaryRepository;

  private DiaryService diaryService;

  @BeforeEach
  void setUp() {
    diaryService =
        new DiaryService(
            diaryRepository, new DiaryProperties(3000L, 2), Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private Diary givenMyDiary(Diary diary) {
    given(diaryRepository.findByIdAndUserIdAndDeletedAtIsNull(diary.getId(), USER_ID))
        .willReturn(Optional.of(diary));
    return diary;
  }

  @Test
  @DisplayName("무료 다이어리가 없는 사용자의 새 다이어리는 무료로 만든다")
  void create_무료이력없음_무료생성() {
    given(diaryRepository.existsByUserIdAndIsFreeTrue(USER_ID)).willReturn(false);

    DiaryCreateResponse response = diaryService.create(USER_ID);

    ArgumentCaptor<Diary> captor = ArgumentCaptor.forClass(Diary.class);
    verify(diaryRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().isFree()).isTrue();
    assertThat(captor.getValue().isPaid()).isFalse();
    assertThat(captor.getValue().getName()).isEqualTo(Diary.DEFAULT_NAME);
    assertThat(response.diaryId()).isEqualTo(captor.getValue().getId());
  }

  @Test
  @DisplayName("무료 다이어리가 이미 있으면(삭제 포함) 새 다이어리는 유료·미결제로 만든다")
  void create_무료이력있음_유료생성() {
    given(diaryRepository.existsByUserIdAndIsFreeTrue(USER_ID)).willReturn(true);

    diaryService.create(USER_ID);

    ArgumentCaptor<Diary> captor = ArgumentCaptor.forClass(Diary.class);
    verify(diaryRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().isFree()).isFalse();
    assertThat(captor.getValue().isPaid()).isFalse();
  }

  @Test
  @DisplayName("이름이 공백뿐이거나 30자를 넘으면 400 DIARY_INVALID_NAME, 정상이면 앞뒤 공백을 지워 저장한다")
  void rename_길이검증() {
    Diary diary = givenMyDiary(Diary.create(USER_ID, true));

    assertThatThrownBy(() -> diaryService.rename(USER_ID, diary.getId(), "   "))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DIARY_INVALID_NAME);
    assertThatThrownBy(() -> diaryService.rename(USER_ID, diary.getId(), "a".repeat(31)))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DIARY_INVALID_NAME);

    diaryService.rename(USER_ID, diary.getId(), "  서울 여행  ");
    assertThat(diary.getName()).isEqualTo("서울 여행");
  }

  @Test
  @DisplayName("삭제하면 deleted_at 이 기록된다")
  void delete_정상_softDelete() {
    Diary diary = givenMyDiary(Diary.create(USER_ID, true));

    diaryService.delete(USER_ID, diary.getId());

    assertThat(diary.getDeletedAt()).isEqualTo(NOW);
  }
}
