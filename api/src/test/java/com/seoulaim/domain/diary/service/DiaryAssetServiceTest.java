package com.seoulaim.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerRequest;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerResponse;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryTemplateRequest;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryTemplateResponse;
import com.seoulaim.domain.diary.dto.DiaryEditorStickersResponse;
import com.seoulaim.domain.diary.entity.DiarySticker;
import com.seoulaim.domain.diary.entity.DiaryStickerCategory;
import com.seoulaim.domain.diary.entity.DiaryTemplate;
import com.seoulaim.domain.diary.entity.TemplateFrame;
import com.seoulaim.domain.diary.repository.DiaryStickerCategoryRepository;
import com.seoulaim.domain.diary.repository.DiaryStickerRepository;
import com.seoulaim.domain.diary.repository.DiaryTemplateRepository;
import com.seoulaim.domain.file.dto.FileResponse;
import com.seoulaim.domain.file.entity.FileStatus;
import com.seoulaim.domain.file.service.FileService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiaryAssetServiceTest {

  private static final UUID IMAGE_FILE_ID = UUID.randomUUID();
  private static final UUID OVERLAY_FILE_ID = UUID.randomUUID();
  private static final String IMAGE_KEY = "diary_template/2026/11/bg.png";
  private static final String OVERLAY_KEY = "diary_template/2026/11/overlay.png";
  private static final List<TemplateFrame> FRAMES =
      List.of(new TemplateFrame("photo-1", 0.1, 0.1, 0.8, 0.4, 0));

  @Mock private DiaryTemplateRepository diaryTemplateRepository;
  @Mock private DiaryStickerRepository diaryStickerRepository;
  @Mock private DiaryStickerCategoryRepository diaryStickerCategoryRepository;
  @Mock private FileService fileService;

  private DiaryAssetService diaryAssetService;

  @BeforeEach
  void setUp() {
    diaryAssetService =
        new DiaryAssetService(
            diaryTemplateRepository,
            diaryStickerRepository,
            diaryStickerCategoryRepository,
            fileService);
  }

  private void givenFile(UUID fileId, String s3Key) {
    given(fileService.getById(fileId))
        .willReturn(new FileResponse(fileId, s3Key, FileStatus.LINKED, "image/png"));
  }

  private static AdminDiaryTemplateRequest templateRequest(
      UUID imageFileId, UUID overlayFileId, Double aspect, List<TemplateFrame> frames) {
    return new AdminDiaryTemplateRequest(
        "Basic", null, imageFileId, 1, null, overlayFileId, aspect, frames);
  }

  @Test
  @DisplayName("레이아웃 필드를 생략하고 템플릿을 만들면 오버레이 없음·기본 비율·빈 프레임으로 저장한다")
  void createTemplate_레이아웃생략_기본값저장() {
    givenFile(IMAGE_FILE_ID, IMAGE_KEY);
    given(diaryTemplateRepository.save(any(DiaryTemplate.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    AdminDiaryTemplateResponse response =
        diaryAssetService.createTemplate(templateRequest(IMAGE_FILE_ID, null, null, null));

    assertThat(response.imageKey()).isEqualTo(IMAGE_KEY);
    assertThat(response.overlayImageKey()).isNull();
    assertThat(response.aspect()).isEqualTo(DiaryTemplate.DEFAULT_ASPECT);
    assertThat(response.frames()).isEmpty();
    assertThat(response.isActive()).isTrue();
  }

  @Test
  @DisplayName("overlayFileId 를 보내면 LINKED 로 바꾸고 오버레이 key·비율·프레임을 저장한다")
  void createTemplate_레이아웃지정_오버레이연결() {
    givenFile(IMAGE_FILE_ID, IMAGE_KEY);
    givenFile(OVERLAY_FILE_ID, OVERLAY_KEY);
    given(diaryTemplateRepository.save(any(DiaryTemplate.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    AdminDiaryTemplateResponse response =
        diaryAssetService.createTemplate(
            templateRequest(IMAGE_FILE_ID, OVERLAY_FILE_ID, 0.75, FRAMES));

    assertThat(response.overlayImageKey()).isEqualTo(OVERLAY_KEY);
    assertThat(response.aspect()).isEqualTo(0.75);
    assertThat(response.frames()).isEqualTo(FRAMES);
    verify(fileService).markLinked(List.of(OVERLAY_FILE_ID));
  }

  @Test
  @DisplayName("수정 시 레이아웃 필드를 생략하면 기존 오버레이·비율·프레임을 유지한다")
  void updateTemplate_레이아웃생략_기존값유지() {
    DiaryTemplate template =
        DiaryTemplate.create("Old", null, "old.png", OVERLAY_KEY, 0.75, FRAMES, 0, true);
    given(diaryTemplateRepository.findById(template.getId())).willReturn(Optional.of(template));
    givenFile(IMAGE_FILE_ID, IMAGE_KEY);

    AdminDiaryTemplateResponse response =
        diaryAssetService.updateTemplate(
            template.getId(), templateRequest(IMAGE_FILE_ID, null, null, null));

    assertThat(response.name()).isEqualTo("Basic");
    assertThat(response.overlayImageKey()).isEqualTo(OVERLAY_KEY);
    assertThat(response.aspect()).isEqualTo(0.75);
    assertThat(response.frames()).isEqualTo(FRAMES);
  }

  @Test
  @DisplayName("수정 시 frames 를 보내면 전체 교체한다")
  void updateTemplate_frames지정_전체교체() {
    DiaryTemplate template =
        DiaryTemplate.create("Old", null, "old.png", null, 0.75, FRAMES, 0, true);
    given(diaryTemplateRepository.findById(template.getId())).willReturn(Optional.of(template));
    givenFile(IMAGE_FILE_ID, IMAGE_KEY);
    List<TemplateFrame> replaced = List.of(new TemplateFrame("p", 0, 0, 1, 1, 90));

    AdminDiaryTemplateResponse response =
        diaryAssetService.updateTemplate(
            template.getId(), templateRequest(IMAGE_FILE_ID, null, null, replaced));

    assertThat(response.frames()).isEqualTo(replaced);
  }

  @Test
  @DisplayName("imageFileId 없이 템플릿을 만들면 COMMON_INVALID_REQUEST(fields: imageFileId) 예외가 발생한다")
  void createTemplate_이미지없음_예외발생() {
    AdminDiaryTemplateRequest request = templateRequest(null, null, null, null);

    assertThatThrownBy(() -> diaryAssetService.createTemplate(request))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);
              assertThat(exception.getFields()).containsKey("imageFileId");
            });
    verify(diaryTemplateRepository, never()).save(any());
  }

  @Test
  @DisplayName("imageFileId 없이 스티커를 만들면 COMMON_INVALID_REQUEST(fields: imageFileId) 예외가 발생한다")
  void createSticker_이미지없음_예외발생() {
    AdminDiaryStickerRequest request = new AdminDiaryStickerRequest("heart", null, 0, null);

    assertThatThrownBy(() -> diaryAssetService.createSticker(request))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_INVALID_REQUEST);
              assertThat(exception.getFields()).containsKey("imageFileId");
            });
    verify(diaryStickerRepository, never()).save(any());
  }

  @Test
  @DisplayName("템플릿 수정 시 imageFileId 를 생략하면 기존 imageKey 를 유지하고 파일을 건드리지 않는다")
  void updateTemplate_이미지생략_기존key유지() {
    DiaryTemplate template = DiaryTemplate.create("Old", null, "old.png", 0, false);
    given(diaryTemplateRepository.findById(template.getId())).willReturn(Optional.of(template));

    AdminDiaryTemplateResponse response =
        diaryAssetService.updateTemplate(template.getId(), templateRequest(null, null, null, null));

    assertThat(response.imageKey()).isEqualTo("old.png");
    assertThat(response.name()).isEqualTo("Basic");
    assertThat(response.isActive()).isFalse();
    verifyNoInteractions(fileService);
  }

  @Test
  @DisplayName("스티커 수정 시 imageFileId 를 생략하면 기존 imageKey 를 유지하고 파일을 건드리지 않는다")
  void updateSticker_이미지생략_기존key유지() {
    DiarySticker sticker = DiarySticker.create("old", "sticker/old.png", 0, true);
    given(diaryStickerRepository.findById(sticker.getId())).willReturn(Optional.of(sticker));

    AdminDiaryStickerResponse response =
        diaryAssetService.updateSticker(
            sticker.getId(), new AdminDiaryStickerRequest("new", null, 3, null));

    assertThat(response.imageKey()).isEqualTo("sticker/old.png");
    assertThat(response.name()).isEqualTo("new");
    assertThat(response.displayOrder()).isEqualTo(3);
    verifyNoInteractions(fileService);
  }

  @Test
  @DisplayName("스티커 수정 시 imageFileId 를 보내면 LINKED 로 바꾸고 key 를 교체한다")
  void updateSticker_이미지지정_key교체() {
    DiarySticker sticker = DiarySticker.create("old", "sticker/old.png", 0, true);
    given(diaryStickerRepository.findById(sticker.getId())).willReturn(Optional.of(sticker));
    givenFile(IMAGE_FILE_ID, "diary_sticker/2026/11/new.png");

    AdminDiaryStickerResponse response =
        diaryAssetService.updateSticker(
            sticker.getId(), new AdminDiaryStickerRequest("new", IMAGE_FILE_ID, 0, null));

    assertThat(response.imageKey()).isEqualTo("diary_sticker/2026/11/new.png");
    verify(fileService).markLinked(List.of(IMAGE_FILE_ID));
  }

  @Test
  @DisplayName("없는 템플릿을 수정하면 DIARY_TEMPLATE_NOT_FOUND 예외가 발생한다")
  void updateTemplate_없는템플릿_예외발생() {
    UUID templateId = UUID.randomUUID();
    given(diaryTemplateRepository.findById(templateId)).willReturn(Optional.empty());
    AdminDiaryTemplateRequest request = templateRequest(null, null, null, null);

    assertThatThrownBy(() -> diaryAssetService.updateTemplate(templateId, request))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.DIARY_TEMPLATE_NOT_FOUND);
  }

  @Test
  @DisplayName("없는 스티커를 수정하면 DIARY_STICKER_NOT_FOUND 예외가 발생한다")
  void updateSticker_없는스티커_예외발생() {
    UUID stickerId = UUID.randomUUID();
    given(diaryStickerRepository.findById(stickerId)).willReturn(Optional.empty());
    AdminDiaryStickerRequest request = new AdminDiaryStickerRequest(null, null, 0, null);

    assertThatThrownBy(() -> diaryAssetService.updateSticker(stickerId, request))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.DIARY_STICKER_NOT_FOUND);
  }

  private void givenCategories(DiaryStickerCategory... categories) {
    given(diaryStickerCategoryRepository.findAllByOrderByDisplayOrderAscIdAsc())
        .willReturn(List.of(categories));
  }

  @Test
  @DisplayName("스티커 등록 시 categoryIds 를 연결하고 응답은 카테고리 노출 순서로 정렬한다 (중복 id 는 합침)")
  void createSticker_카테고리지정_연결후정렬() {
    DiaryStickerCategory cute = DiaryStickerCategory.create("Cute", 0);
    DiaryStickerCategory food = DiaryStickerCategory.create("Food", 1);
    given(diaryStickerCategoryRepository.findAllById(any())).willReturn(List.of(food, cute));
    givenCategories(cute, food);
    givenFile(IMAGE_FILE_ID, "diary_sticker/2026/11/heart.png");
    given(diaryStickerRepository.save(any(DiarySticker.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    AdminDiaryStickerResponse response =
        diaryAssetService.createSticker(
            new AdminDiaryStickerRequest(
                "heart",
                IMAGE_FILE_ID,
                0,
                null,
                List.of(food.getId(), cute.getId(), food.getId())));

    assertThat(response.categoryIds()).containsExactly(cute.getId(), food.getId());
  }

  @Test
  @DisplayName("스티커 등록 시 없는 카테고리 id 가 있으면 DIARY_STICKER_CATEGORY_NOT_FOUND 이고 파일·저장을 건드리지 않는다")
  void createSticker_없는카테고리_예외() {
    DiaryStickerCategory cute = DiaryStickerCategory.create("Cute", 0);
    UUID missing = UUID.randomUUID();
    given(diaryStickerCategoryRepository.findAllById(any())).willReturn(List.of(cute));
    AdminDiaryStickerRequest request =
        new AdminDiaryStickerRequest(
            "heart", IMAGE_FILE_ID, 0, null, List.of(cute.getId(), missing));

    assertThatThrownBy(() -> diaryAssetService.createSticker(request))
        .isInstanceOfSatisfying(
            BusinessException.class,
            e -> {
              assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DIARY_STICKER_CATEGORY_NOT_FOUND);
              assertThat(e.getFields()).containsEntry("categoryIds", missing.toString());
            });
    verifyNoInteractions(fileService);
    verify(diaryStickerRepository, never()).save(any());
  }

  @Test
  @DisplayName("스티커 수정 시 categoryIds 를 생략하면 기존 카테고리 연결을 유지한다")
  void updateSticker_카테고리생략_기존유지() {
    DiaryStickerCategory cute = DiaryStickerCategory.create("Cute", 0);
    DiarySticker sticker = DiarySticker.create("old", "sticker/old.png", 0, true);
    sticker.replaceCategories(List.of(cute.getId()));
    given(diaryStickerRepository.findById(sticker.getId())).willReturn(Optional.of(sticker));
    givenCategories(cute);

    AdminDiaryStickerResponse response =
        diaryAssetService.updateSticker(
            sticker.getId(), new AdminDiaryStickerRequest("new", null, 0, null, null));

    assertThat(response.categoryIds()).containsExactly(cute.getId());
    verify(diaryStickerCategoryRepository, never()).findAllById(any());
  }

  @Test
  @DisplayName("스티커 수정 시 categoryIds 가 빈 배열이면 카테고리 연결을 전부 해제한다")
  void updateSticker_카테고리빈배열_전부해제() {
    DiaryStickerCategory cute = DiaryStickerCategory.create("Cute", 0);
    DiarySticker sticker = DiarySticker.create("old", "sticker/old.png", 0, true);
    sticker.replaceCategories(List.of(cute.getId()));
    given(diaryStickerRepository.findById(sticker.getId())).willReturn(Optional.of(sticker));

    AdminDiaryStickerResponse response =
        diaryAssetService.updateSticker(
            sticker.getId(), new AdminDiaryStickerRequest("new", null, 0, null, List.of()));

    assertThat(response.categoryIds()).isEmpty();
    assertThat(sticker.getCategoryIds()).isEmpty();
  }

  @Test
  @DisplayName("회원 스티커 조회는 카테고리 전체와 활성 스티커(categoryIds 포함)를 함께 반환한다")
  void getEditorStickers_정상_카테고리와스티커반환() {
    DiaryStickerCategory cute = DiaryStickerCategory.create("Cute", 0);
    DiaryStickerCategory food = DiaryStickerCategory.create("Food", 1);
    givenCategories(cute, food);
    DiarySticker heart = DiarySticker.create("heart", "sticker/heart.png", 0, true);
    heart.replaceCategories(List.of(food.getId(), cute.getId()));
    DiarySticker star = DiarySticker.create("star", "sticker/star.png", 1, true);
    given(diaryStickerRepository.findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc())
        .willReturn(List.of(heart, star));

    DiaryEditorStickersResponse response = diaryAssetService.getEditorStickers();

    assertThat(response.categories()).extracting("name").containsExactly("Cute", "Food");
    assertThat(response.stickers()).extracting("name").containsExactly("heart", "star");
    assertThat(response.stickers().get(0).categoryIds())
        .containsExactly(cute.getId(), food.getId());
    assertThat(response.stickers().get(1).categoryIds()).isEmpty();
  }
}
