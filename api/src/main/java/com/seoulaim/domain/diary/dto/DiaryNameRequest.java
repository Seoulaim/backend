package com.seoulaim.domain.diary.dto;

/** 길이 검증(앞뒤 공백 제거 후 1~30자)은 DIARY_INVALID_NAME 으로 내려야 해서 서비스에서 한다. */
public record DiaryNameRequest(String name) {}
