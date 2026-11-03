package com.seoulaim.domain.diary.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** 완성 페이지 이미지 fileId. 배열 순서가 페이지 순서다. */
public record DiaryFinishedPagesRequest(@NotEmpty @Size(max = 200) List<@NotNull UUID> fileIds) {}
