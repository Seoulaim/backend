package com.seoulaim.domain.diary.dto;

import com.seoulaim.domain.diary.entity.DiaryDownloadFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 다운로드 기록.
 *
 * @param format PDF | JPG (대소문자 무시)
 * @param pageCount 내려받은 쪽 수
 */
public record DiaryDownloadRequest(
    @NotNull DiaryDownloadFormat format, @NotNull @Min(1) @Max(1000) Integer pageCount) {}
