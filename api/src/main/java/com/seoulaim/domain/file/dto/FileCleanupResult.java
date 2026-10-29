package com.seoulaim.domain.file.dto;

/** 정리 대상 건수와 실제 삭제 건수. S3 삭제에 실패한 행은 남겨 두고 다음 배치에서 재시도한다. */
public record FileCleanupResult(int targetCount, int deletedCount) {}
