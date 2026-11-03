package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.diary.entity.Diary;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 편집 화면 진입 데이터.
 *
 * @param pageKeys 완성 페이지 key. 아직 없으면 null
 * @param usedTemplates spreads 가 쓰는 템플릿 중 비활성화된 것 (활성 목록 API 에 안 나오므로 따로 내려준다)
 */
public record DiaryEditorResponse(
    DiaryInfo diary,
    List<String> pageKeys,
    List<Object> spreads,
    Map<String, Object> placePhotos,
    List<DiaryTemplateResponse> usedTemplates) {

  public static DiaryEditorResponse of(Diary diary, List<DiaryTemplateResponse> usedTemplates) {
    return new DiaryEditorResponse(
        new DiaryInfo(
            diary.getId(), diary.getName(), diary.isFinalized(), diary.isFree(), diary.isPaid()),
        diary.getPageKeys(),
        diary.getSpreads(),
        diary.getPlacePhotos(),
        usedTemplates);
  }

  public record DiaryInfo(UUID id, String name, boolean finalized, boolean free, boolean paid) {}
}
