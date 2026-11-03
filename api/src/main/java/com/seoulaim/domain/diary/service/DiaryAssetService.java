package com.seoulaim.domain.diary.service;

import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerRequest;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryStickerResponse;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryTemplateRequest;
import com.seoulaim.domain.diary.admin.dto.AdminDiaryTemplateResponse;
import com.seoulaim.domain.diary.dto.DiaryStickerResponse;
import com.seoulaim.domain.diary.dto.DiaryTemplateResponse;
import com.seoulaim.domain.diary.entity.DiarySticker;
import com.seoulaim.domain.diary.entity.DiaryTemplate;
import com.seoulaim.domain.diary.repository.DiaryStickerRepository;
import com.seoulaim.domain.diary.repository.DiaryTemplateRepository;
import com.seoulaim.domain.file.service.FileService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 다이어리 편집에 쓰는 템플릿·스티커. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryAssetService {

  private final DiaryTemplateRepository diaryTemplateRepository;
  private final DiaryStickerRepository diaryStickerRepository;
  private final FileService fileService;

  public List<DiaryTemplateResponse> getActiveTemplates() {
    return diaryTemplateRepository.findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc().stream()
        .map(DiaryTemplateResponse::from)
        .toList();
  }

  public List<DiaryStickerResponse> getActiveStickers() {
    return diaryStickerRepository.findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc().stream()
        .map(DiaryStickerResponse::from)
        .toList();
  }

  public List<AdminDiaryTemplateResponse> getAllTemplates() {
    return diaryTemplateRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
        .map(AdminDiaryTemplateResponse::from)
        .toList();
  }

  public List<AdminDiaryStickerResponse> getAllStickers() {
    return diaryStickerRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
        .map(AdminDiaryStickerResponse::from)
        .toList();
  }

  @Transactional
  public AdminDiaryTemplateResponse createTemplate(AdminDiaryTemplateRequest request) {
    DiaryTemplate template =
        diaryTemplateRepository.save(
            DiaryTemplate.create(
                request.name(),
                request.description(),
                linkRequiredImage(request.imageFileId()),
                request.overlayFileId() == null ? null : linkImage(request.overlayFileId()),
                request.aspect() == null ? DiaryTemplate.DEFAULT_ASPECT : request.aspect(),
                request.frames() == null ? List.of() : request.frames(),
                request.displayOrder(),
                request.isActive() == null || request.isActive()));
    return AdminDiaryTemplateResponse.from(template);
  }

  /** 이미지를 교체하면 이전 파일은 LINKED 로 남는다. imageFileId·레이아웃 필드는 생략하면 기존 값을 유지한다. */
  @Transactional
  public AdminDiaryTemplateResponse updateTemplate(
      UUID templateId, AdminDiaryTemplateRequest request) {
    DiaryTemplate template =
        diaryTemplateRepository
            .findById(templateId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_TEMPLATE_NOT_FOUND));
    template.update(
        request.name(),
        request.description(),
        request.imageFileId() == null ? template.getImageKey() : linkImage(request.imageFileId()),
        request.overlayFileId() == null
            ? template.getOverlayImageKey()
            : linkImage(request.overlayFileId()),
        request.aspect() == null ? template.getAspect() : request.aspect(),
        request.frames() == null ? template.getFrames() : request.frames(),
        request.displayOrder(),
        request.isActive() == null ? template.isActive() : request.isActive());
    return AdminDiaryTemplateResponse.from(template);
  }

  @Transactional
  public AdminDiaryStickerResponse createSticker(AdminDiaryStickerRequest request) {
    DiarySticker sticker =
        diaryStickerRepository.save(
            DiarySticker.create(
                request.name(),
                linkRequiredImage(request.imageFileId()),
                request.displayOrder(),
                request.isActive() == null || request.isActive()));
    return AdminDiaryStickerResponse.from(sticker);
  }

  /** imageFileId 를 생략하면 기존 이미지를 유지한다. */
  @Transactional
  public AdminDiaryStickerResponse updateSticker(UUID stickerId, AdminDiaryStickerRequest request) {
    DiarySticker sticker =
        diaryStickerRepository
            .findById(stickerId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_STICKER_NOT_FOUND));
    sticker.update(
        request.name(),
        request.imageFileId() == null ? sticker.getImageKey() : linkImage(request.imageFileId()),
        request.displayOrder(),
        request.isActive() == null ? sticker.isActive() : request.isActive());
    return AdminDiaryStickerResponse.from(sticker);
  }

  // 생성 시 imageFileId 는 필수. 수정에선 선택이라 DTO 가 아니라 여기서 검증한다
  private String linkRequiredImage(UUID fileId) {
    if (fileId == null) {
      throw new BusinessException(
          ErrorCode.COMMON_INVALID_REQUEST, Map.of("imageFileId", "생성 시 필수입니다"));
    }
    return linkImage(fileId);
  }

  // 업로드된 파일을 LINKED 로 바꾸고 s3 key 를 꺼낸다. 없는 fileId 면 FILE_NOT_FOUND
  private String linkImage(UUID fileId) {
    fileService.markLinked(List.of(fileId));
    return fileService.getById(fileId).s3Key();
  }
}
