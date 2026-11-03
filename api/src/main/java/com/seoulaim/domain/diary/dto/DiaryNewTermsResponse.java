package com.seoulaim.domain.diary.dto;

/**
 * 새 다이어리 생성 조건.
 *
 * @param free 다음에 만들 다이어리가 무료인지
 * @param price 유료 다이어리 가격 (원)
 */
public record DiaryNewTermsResponse(boolean free, long price) {}
