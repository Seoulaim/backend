package com.seoulaim.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulaim.domain.diary.dto.DiaryCreateResponse;
import com.seoulaim.domain.diary.dto.DiaryDownloadRequest;
import com.seoulaim.domain.diary.dto.DiaryEditorResponse;
import com.seoulaim.domain.diary.dto.DiaryFinishedPagesRequest;
import com.seoulaim.domain.diary.dto.DiaryFinishedPagesResponse;
import com.seoulaim.domain.diary.dto.DiarySpreadsRequest;
import com.seoulaim.domain.diary.dto.DiaryUsedTemplateResponse;
import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.entity.DiaryDownload;
import com.seoulaim.domain.diary.entity.DiaryDownloadFormat;
import com.seoulaim.domain.diary.entity.DiaryTemplate;
import com.seoulaim.domain.diary.entity.TemplateFrame;
import com.seoulaim.domain.diary.repository.DiaryDownloadRepository;
import com.seoulaim.domain.diary.repository.DiaryRepository;
import com.seoulaim.domain.file.dto.FileDetail;
import com.seoulaim.domain.file.entity.FileDomain;
import com.seoulaim.domain.file.entity.FileStatus;
import com.seoulaim.domain.file.service.FileService;
import com.seoulaim.global.config.DiaryProperties;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
  private static final long PRICE = 3000L;
  private static final int FREE_SPREADS = 2;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock private DiaryRepository diaryRepository;
  @Mock private DiaryDownloadRepository diaryDownloadRepository;
  @Mock private DiaryAssetService diaryAssetService;
  @Mock private FileService fileService;

  private DiaryService diaryService;

  @BeforeEach
  void setUp() {
    diaryService =
        new DiaryService(
            diaryRepository,
            diaryDownloadRepository,
            diaryAssetService,
            fileService,
            new DiaryProperties(PRICE, FREE_SPREADS),
            objectMapper,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private Diary givenMyDiary(Diary diary) {
    given(diaryRepository.findByIdAndUserIdAndDeletedAtIsNull(diary.getId(), USER_ID))
        .willReturn(Optional.of(diary));
    return diary;
  }

  private DiarySpreadsRequest spreads(int count) {
    return request(spreadsJson(count), "{}");
  }

  private static String spreadsJson(int count) {
    StringBuilder json = new StringBuilder("[");
    for (int i = 0; i < count; i++) {
      json.append(i == 0 ? "" : ",").append("{\"left\":{},\"right\":{}}");
    }
    return json.append(']').toString();
  }

  private DiarySpreadsRequest request(String spreadsJson, String placePhotosJson) {
    try {
      return new DiarySpreadsRequest(
          objectMapper.readTree(spreadsJson), objectMapper.readTree(placePhotosJson));
    } catch (Exception exception) {
      throw new IllegalArgumentException(exception);
    }
  }

  private static FileDetail file(UUID fileId, UUID uploaderId, String key) {
    return new FileDetail(
        fileId, uploaderId, FileDomain.DIARY, key, "image/jpeg", FileStatus.PENDING);
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
  @DisplayName("무료 다이어리는 무료 범위(2 spread)까지 저장되고 last_edited_at 이 갱신된다")
  void saveSpreads_무료2쪽_저장() {
    Diary diary = givenMyDiary(Diary.create(USER_ID, true));

    diaryService.saveSpreads(USER_ID, diary.getId(), spreads(FREE_SPREADS));

    assertThat(diary.getSpreads()).hasSize(FREE_SPREADS);
    assertThat(diary.getLastEditedAt()).isEqualTo(NOW);
    assertThat(diary.getSaveCount()).isEqualTo(1);
    assertThat(diary.getLastSavedAt()).isEqualTo(NOW);
  }

  @Test
  @DisplayName("무료 다이어리라도 무료 범위를 넘는 3 spread 는 402 DIARY_PAYMENT_REQUIRED")
  void saveSpreads_무료3쪽_402() {
    Diary diary = givenMyDiary(Diary.create(USER_ID, true));

    assertThatThrownBy(() -> diaryService.saveSpreads(USER_ID, diary.getId(), spreads(3)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DIARY_PAYMENT_REQUIRED);
    assertThat(diary.getSpreads()).isEmpty();
  }

  @Test
  @DisplayName("결제된 무료 다이어리는 무료 범위를 넘어도 저장된다")
  void saveSpreads_무료결제후3쪽_저장() {
    Diary diary = Diary.create(USER_ID, true);
    diary.markPaid();
    givenMyDiary(diary);

    diaryService.saveSpreads(USER_ID, diary.getId(), spreads(3));

    assertThat(diary.getSpreads()).hasSize(3);
  }

  @Test
  @DisplayName("두 번째(유료) 다이어리는 결제 전이면 1 spread 도 402 DIARY_PAYMENT_REQUIRED")
  void saveSpreads_유료미결제_402() {
    Diary diary = givenMyDiary(Diary.create(USER_ID, false));

    assertThatThrownBy(() -> diaryService.saveSpreads(USER_ID, diary.getId(), spreads(1)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DIARY_PAYMENT_REQUIRED);
  }

  @Test
  @DisplayName("FINALIZED 다이어리에 저장하면 409 DIARY_FINALIZED")
  void saveSpreads_완성후_409() {
    Diary diary = Diary.create(USER_ID, true);
    diary.finalizeWith(List.of(), Map.of(), 0, NOW);
    givenMyDiary(diary);

    assertThatThrownBy(() -> diaryService.saveSpreads(USER_ID, diary.getId(), spreads(1)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DIARY_FINALIZED);
  }

  @Test
  @DisplayName("spreads 가 배열이 아니면 400 COMMON_INVALID_REQUEST")
  void saveSpreads_배열아님_400() {
    Diary diary = givenMyDiary(Diary.create(USER_ID, true));

    assertThatThrownBy(() -> diaryService.saveSpreads(USER_ID, diary.getId(), request("{}", "{}")))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);
  }

  @Test
  @DisplayName("편집 데이터가 2MB 를 넘으면 400 DIARY_CONTENT_TOO_LARGE")
  void saveSpreads_2MB초과_400() {
    Diary diary = Diary.create(USER_ID, true);
    diary.markPaid();
    givenMyDiary(diary);
    String big = "\"" + "a".repeat(DiaryService.MAX_EDIT_DATA_BYTES) + "\"";

    assertThatThrownBy(
            () -> diaryService.saveSpreads(USER_ID, diary.getId(), request("[" + big + "]", "{}")))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DIARY_CONTENT_TOO_LARGE);
  }

  @Test
  @DisplayName("placePhotos 의 사진 fileId 를 LINKED 처리하고 그대로 저장한다")
  void saveSpreads_장소사진_링크() {
    Diary diary = givenMyDiary(Diary.create(USER_ID, true));
    UUID fileId = UUID.randomUUID();
    String key = "diary/2026/11/p.jpg";
    given(fileService.findDetails(List.of(fileId))).willReturn(List.of(file(fileId, USER_ID, key)));

    diaryService.saveSpreads(
        USER_ID,
        diary.getId(),
        request("[]", "{\"other\":[{\"id\":\"%s\",\"key\":\"%s\"}]}".formatted(fileId, key)));

    verify(fileService).markLinked(List.of(fileId));
    assertThat(diary.getPlacePhotos()).containsOnlyKeys("other");
  }

  @Test
  @DisplayName("저장할 때마다 save_count 가 늘고, photo_count 는 placePhotos 전체 항목 수(같은 사진 중복 포함)로 갱신된다")
  void saveSpreads_이력_저장횟수와사진수() {
    Diary diary = givenMyDiary(Diary.create(USER_ID, true));
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    given(fileService.findDetails(anyList()))
        .willReturn(
            List.of(file(first, USER_ID, "diary/a.jpg"), file(second, USER_ID, "diary/b.jpg")));
    String photos =
        """
        {"p1":[{"id":"%s","key":"diary/a.jpg"},{"id":"%s","key":"diary/b.jpg"}],
         "other":[{"id":"%s","key":"diary/a.jpg"}]}
        """
            .formatted(first, second, first);

    diaryService.saveSpreads(USER_ID, diary.getId(), request("[]", photos));
    diaryService.saveSpreads(USER_ID, diary.getId(), request("[]", photos));

    assertThat(diary.getSaveCount()).isEqualTo(2);
    assertThat(diary.getPhotoCount()).isEqualTo(3);
    assertThat(diary.getLastSavedAt()).isEqualTo(NOW);
  }

  @Test
  @DisplayName("완성 전 다이어리의 다운로드 기록은 409 DIARY_NOT_FINISHED 이고 저장하지 않는다")
  void recordDownload_미완성_409() {
    Diary diary = givenMyDiary(Diary.create(USER_ID, true));

    assertThatThrownBy(
            () ->
                diaryService.recordDownload(
                    USER_ID, diary.getId(), new DiaryDownloadRequest(DiaryDownloadFormat.PDF, 4)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DIARY_NOT_FINISHED);
    verify(diaryDownloadRepository, never()).save(any());
  }

  @Test
  @DisplayName("완성 다이어리의 다운로드를 형식·쪽 수와 함께 기록한다")
  void recordDownload_완성_기록() {
    Diary diary = Diary.create(USER_ID, true);
    diary.finalizeWith(List.of(), Map.of(), 0, NOW);
    givenMyDiary(diary);

    diaryService.recordDownload(
        USER_ID, diary.getId(), new DiaryDownloadRequest(DiaryDownloadFormat.JPG, 6));

    ArgumentCaptor<DiaryDownload> captor = ArgumentCaptor.forClass(DiaryDownload.class);
    verify(diaryDownloadRepository).save(captor.capture());
    assertThat(captor.getValue().getDiaryId()).isEqualTo(diary.getId());
    assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
    assertThat(captor.getValue().getFormat()).isEqualTo(DiaryDownloadFormat.JPG);
    assertThat(captor.getValue().getPageCount()).isEqualTo(6);
  }

  @Test
  @DisplayName("placePhotos 에 없는 fileId 가 있으면 400 이고 아무것도 LINKED 하지 않는다")
  void saveSpreads_없는사진_400() {
    Diary diary = givenMyDiary(Diary.create(USER_ID, true));
    UUID fileId = UUID.randomUUID();
    given(fileService.findDetails(List.of(fileId))).willReturn(List.of());

    assertThatThrownBy(
            () ->
                diaryService.saveSpreads(
                    USER_ID,
                    diary.getId(),
                    request("[]", "{\"p1\":[{\"id\":\"%s\",\"key\":\"k\"}]}".formatted(fileId))))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);
    verify(fileService, never()).markLinked(anyList());
  }

  @Test
  @DisplayName("finalize 는 저장 후 FINALIZED 로 바꾸고 completed_at 을 기록한다")
  void finalizeDiary_정상_완성() {
    Diary diary = givenMyDiary(Diary.create(USER_ID, true));

    diaryService.finalizeDiary(USER_ID, diary.getId(), spreads(2));

    assertThat(diary.isFinalized()).isTrue();
    assertThat(diary.getCompletedAt()).isEqualTo(NOW);
    assertThat(diary.getSpreads()).hasSize(2);
  }

  @Test
  @DisplayName("완성 전 finished-pages 를 호출하면 409 DIARY_NOT_FINISHED")
  void saveFinishedPages_완성전_409() {
    Diary diary = givenMyDiary(Diary.create(USER_ID, true));

    assertThatThrownBy(
            () ->
                diaryService.saveFinishedPages(
                    USER_ID,
                    diary.getId(),
                    new DiaryFinishedPagesRequest(List.of(UUID.randomUUID()))))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DIARY_NOT_FINISHED);
    verify(fileService, never()).markLinked(anyList());
  }

  @Test
  @DisplayName("finished-pages 는 fileId 순서대로 key 를 저장하고 편집 데이터를 비운다")
  void saveFinishedPages_정상_키저장및편집데이터비움() {
    Diary diary = Diary.create(USER_ID, true);
    diary.finalizeWith(List.of(Map.of("a", 1)), Map.of("other", List.of()), 0, NOW);
    givenMyDiary(diary);
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    given(fileService.findDetails(List.of(first, second)))
        .willReturn(
            List.of(file(second, USER_ID, "diary/2.jpg"), file(first, USER_ID, "diary/1.jpg")));

    DiaryFinishedPagesResponse response =
        diaryService.saveFinishedPages(
            USER_ID, diary.getId(), new DiaryFinishedPagesRequest(List.of(first, second)));

    assertThat(response.pageKeys()).containsExactly("diary/1.jpg", "diary/2.jpg");
    assertThat(diary.getSpreads()).isEmpty();
    assertThat(diary.getPlacePhotos()).isEmpty();
    assertThat(diary.thumbnailKey()).isEqualTo("diary/1.jpg");
    verify(fileService).markLinked(List.of(first, second));
  }

  @Test
  @DisplayName("이미 완성 페이지가 있으면 기존 key 를 그대로 반환한다")
  void saveFinishedPages_이미있음_기존반환() {
    Diary diary = Diary.create(USER_ID, true);
    diary.finalizeWith(List.of(), Map.of(), 0, NOW);
    diary.completePages(List.of("diary/old.jpg"));
    givenMyDiary(diary);

    DiaryFinishedPagesResponse response =
        diaryService.saveFinishedPages(
            USER_ID, diary.getId(), new DiaryFinishedPagesRequest(List.of(UUID.randomUUID())));

    assertThat(response.pageKeys()).containsExactly("diary/old.jpg");
    verify(fileService, never()).findDetails(any());
  }

  @Test
  @DisplayName("남이 올린 파일로 finished-pages 를 등록하면 400")
  void saveFinishedPages_타인파일_400() {
    Diary diary = Diary.create(USER_ID, true);
    diary.finalizeWith(List.of(), Map.of(), 0, NOW);
    givenMyDiary(diary);
    UUID fileId = UUID.randomUUID();
    given(fileService.findDetails(List.of(fileId)))
        .willReturn(List.of(file(fileId, UUID.randomUUID(), "diary/x.jpg")));

    assertThatThrownBy(
            () ->
                diaryService.saveFinishedPages(
                    USER_ID, diary.getId(), new DiaryFinishedPagesRequest(List.of(fileId))))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);
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

  @Test
  @DisplayName("없거나 남의(또는 삭제된) 다이어리는 404 DIARY_NOT_FOUND")
  void getEditor_없음_404() {
    UUID diaryId = UUID.randomUUID();
    given(diaryRepository.findByIdAndUserIdAndDeletedAtIsNull(diaryId, USER_ID))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> diaryService.getEditor(USER_ID, diaryId))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DIARY_NOT_FOUND);
  }

  @Test
  @DisplayName("편집 화면은 spreads 안 어디에 있든 templateId 를 모아 비활성 템플릿을 조회한다")
  void getEditor_templateId수집() throws Exception {
    UUID nested = UUID.randomUUID();
    UUID top = UUID.randomUUID();
    Diary diary = Diary.create(USER_ID, true);
    JsonNode spreads =
        objectMapper.readTree(
            """
            [{"templateId": "%s", "left": {"templateId": "%s"}, "right": {"templateId": "not-uuid"}}]
            """
                .formatted(top, nested));
    diary.saveSpreads(objectMapper.convertValue(spreads, List.class), Map.of(), 0, NOW);
    givenMyDiary(diary);
    given(diaryAssetService.getInactiveTemplates(Set.of(top, nested))).willReturn(List.of());

    DiaryEditorResponse response = diaryService.getEditor(USER_ID, diary.getId());

    assertThat(response.diary().free()).isTrue();
    assertThat(response.diary().finalized()).isFalse();
    assertThat(response.pageKeys()).isNull();
    assertThat(response.usedTemplates()).isEmpty();
  }

  @Test
  @DisplayName("비활성 템플릿을 쓴 다이어리의 편집 화면 응답 usedTemplates 에 그 템플릿 전체 정보가 포함된다")
  void getEditor_비활성템플릿사용_usedTemplates포함() throws Exception {
    DiaryTemplate hidden =
        DiaryTemplate.create(
            "Hidden",
            "숨긴 템플릿",
            "diary_template/2026/11/bg.png",
            "diary_template/2026/11/overlay.png",
            0.75,
            List.of(new TemplateFrame("photo-1", 0.1, 0.1, 0.8, 0.4, 0)),
            0,
            false);
    Diary diary = Diary.create(USER_ID, true);
    JsonNode spreads =
        objectMapper.readTree("[{\"templateId\": \"%s\"}]".formatted(hidden.getId()));
    diary.saveSpreads(objectMapper.convertValue(spreads, List.class), Map.of(), 0, NOW);
    givenMyDiary(diary);
    given(diaryAssetService.getInactiveTemplates(Set.of(hidden.getId())))
        .willReturn(List.of(DiaryUsedTemplateResponse.from(hidden)));

    DiaryEditorResponse response = diaryService.getEditor(USER_ID, diary.getId());

    assertThat(response.usedTemplates())
        .singleElement()
        .satisfies(
            t -> {
              assertThat(t.id()).isEqualTo(hidden.getId());
              assertThat(t.name()).isEqualTo("Hidden");
              assertThat(t.imageKey()).isEqualTo("diary_template/2026/11/bg.png");
              assertThat(t.overlayImageKey()).isEqualTo("diary_template/2026/11/overlay.png");
              assertThat(t.aspect()).isEqualTo(0.75);
              assertThat(t.frames()).hasSize(1);
              assertThat(t.isActive()).isFalse();
            });
    JsonNode json = objectMapper.valueToTree(response.usedTemplates().get(0));
    assertThat(json.get("isActive").asBoolean(true)).isFalse();
  }
}
