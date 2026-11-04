package com.seoulaim.domain.content.service;

import com.seoulaim.domain.file.service.FileService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 콘텐츠 이미지 교체 규칙 (다이어리 템플릿과 동일 + 제거)
 *
 * <ul>
 *   <li>imageFileId 있음 → LINKED 처리 후 그 s3 key (없는 파일이면 FILE_NOT_FOUND)
 *   <li>removeImage=true → null
 *   <li>둘 다 없음 → 기존 key 유지 (생성이면 null)
 *   <li>둘 다 보냄 → 400
 * </ul>
 *
 * <p>교체·제거된 이전 파일은 LINKED 로 남는다 (S3 삭제는 TODO, 템플릿과 동일)
 */
@Component
@RequiredArgsConstructor
class ContentImageResolver {

  private final FileService fileService;

  String resolve(String currentKey, UUID imageFileId, Boolean removeImage) {
    boolean remove = Boolean.TRUE.equals(removeImage);
    if (remove && imageFileId != null) {
      throw LocalizedTexts.invalid("removeImage", "imageFileId 와 함께 보낼 수 없습니다");
    }
    if (remove) {
      return null;
    }
    if (imageFileId == null) {
      return currentKey;
    }
    fileService.markLinked(List.of(imageFileId));
    return fileService.getById(imageFileId).s3Key();
  }
}
