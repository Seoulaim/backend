package com.seoulaim.domain.diary.dto;

import java.util.List;

/** 회원 편집기 스티커 피커. 둘 다 어드민이 정한 순서. "All" 탭은 앱이 붙인다. */
public record DiaryEditorStickersResponse(
    List<DiaryStickerCategoryResponse> categories, List<DiaryStickerResponse> stickers) {}
