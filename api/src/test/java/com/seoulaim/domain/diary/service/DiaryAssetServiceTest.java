package com.seoulaim.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.diary.admin.dto.AdminDiaryTemplateRequest;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryTemplateResponse;
import com.seoulaim.domain.diary.entity.DiaryTemplate;
import com.seoulaim.domain.diary.entity.TemplateFrame;
import com.seoulaim.domain.diary.repository.DiaryStickerRepository;
import com.seoulaim.domain.diary.repository.DiaryTemplateRepository;
import com.seoulaim.domain.file.dto.FileResponse;
import com.seoulaim.domain.file.entity.FileStatus;
import com.seoulaim.domain.file.service.FileService;
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
  @Mock private FileService fileService;

  private DiaryAssetService diaryAssetService;

  @BeforeEach
  void setUp() {
    diaryAssetService =
        new DiaryAssetService(diaryTemplateRepository, diaryStickerRepository, fileService);
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
}
