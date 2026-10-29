package com.seoulaim.domain.diary.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

/** content 는 JSON object 만 받는다. 배열·문자열이면 역직렬화 단계에서 400 이 된다. */
public record DiarySubmitRequest(
    @NotNull UUID orderItemId,
    @NotNull @Min(1) @Max(30) Integer pageCount,
    @NotNull Map<String, Object> content,
    @NotNull UUID pdfFileId) {}
