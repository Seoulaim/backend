package com.seoulaim.domain.diary.admin.dto;

/** 카테고리 생성·수정. name 검증(공백·50자 초과 → DIARY_STICKER_CATEGORY_INVALID)은 서비스에서 한다. */
public record AdminDiaryStickerCategoryRequest(String name) {}
