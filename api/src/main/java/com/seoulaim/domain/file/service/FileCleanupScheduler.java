package com.seoulaim.domain.file.service;

import com.seoulaim.domain.file.dto.FileCleanupResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupScheduler {

  private final FileService fileService;

  // TODO: 인스턴스가 여러 대가 되면 ShedLock 등으로 단일 실행 보장 (지금은 중복 실행돼도 결과는 같다)
  @Scheduled(cron = "0 0 4 * * *", zone = "UTC")
  public void deleteStalePendingFiles() {
    try {
      FileCleanupResult result = fileService.deleteStalePendingFiles();
      log.info("PENDING 파일 정리 완료: 대상 {}건, 삭제 {}건", result.targetCount(), result.deletedCount());
    } catch (RuntimeException exception) {
      log.error("PENDING 파일 정리 실패", exception);
    }
  }
}
