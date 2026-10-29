package com.seoulaim.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.file.dto.FileCleanupResult;
import com.seoulaim.domain.file.dto.PresignedUploadRequest;
import com.seoulaim.domain.file.dto.PresignedUploadResponse;
import com.seoulaim.domain.file.entity.File;
import com.seoulaim.domain.file.entity.FileDomain;
import com.seoulaim.domain.file.entity.FileStatus;
import com.seoulaim.domain.file.repository.FileRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-01T03:00:00Z");
  private static final UUID UPLOADER_ID = UUID.randomUUID();

  @Mock private FileRepository fileRepository;
  @Mock private FileStorage fileStorage;

  private FileService fileService;

  @BeforeEach
  void setUp() {
    fileService = new FileService(fileRepository, fileStorage, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  @DisplayName("presigned 업로드를 요청하면 도메인소문자/yyyy/MM/uuid.확장자 규칙으로 key 를 만든다")
  void createPresignedUpload_정상요청_규칙대로key생성() throws Exception {
    given(fileRepository.save(any(File.class))).willAnswer(invocation -> invocation.getArgument(0));
    given(fileStorage.presignPut(anyString(), anyString(), anyLong(), any()))
        .willReturn(
            URI.create("https://seoulaim-dev-591553897454.s3.ap-northeast-2.amazonaws.com/signed")
                .toURL());

    PresignedUploadResponse response =
        fileService.createPresignedUpload(
            UPLOADER_ID, new PresignedUploadRequest("DIARY", "image/jpeg", 1024L));

    assertThat(response.s3Key())
        .matches(
            "diary/2026/10/[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg");
    assertThat(response.expiresAt()).isEqualTo(NOW.plus(FileService.UPLOAD_URL_EXPIRATION));
    verify(fileStorage)
        .presignPut(response.s3Key(), "image/jpeg", 1024L, FileService.UPLOAD_URL_EXPIRATION);
  }

  @Test
  @DisplayName("허용되지 않은 contentType 이면 FILE_INVALID_CONTENT_TYPE 예외가 발생한다")
  void createPresignedUpload_허용안된contentType_예외발생() {
    PresignedUploadRequest request = new PresignedUploadRequest("REVIEW", "image/gif", 1024L);

    assertThatThrownBy(() -> fileService.createPresignedUpload(UPLOADER_ID, request))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.FILE_INVALID_CONTENT_TYPE);
    verify(fileRepository, never()).save(any());
  }

  @Test
  @DisplayName("파일 크기가 10MB 를 넘으면 FILE_TOO_LARGE 예외가 발생한다")
  void createPresignedUpload_10MB초과_예외발생() {
    PresignedUploadRequest request =
        new PresignedUploadRequest("REVIEW", "image/png", FileService.MAX_SIZE_BYTES + 1);

    assertThatThrownBy(() -> fileService.createPresignedUpload(UPLOADER_ID, request))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.FILE_TOO_LARGE);
    verify(fileRepository, never()).save(any());
  }

  @Test
  @DisplayName("이미 LINKED 인 파일이 섞여 있어도 markLinked 는 예외 없이 전부 LINKED 로 만든다")
  void markLinked_이미LINKED포함_무시하고연결() {
    File pending = File.createPending(UPLOADER_ID, FileDomain.REVIEW, "review/a.jpg", null, null);
    File linked = File.createPending(UPLOADER_ID, FileDomain.REVIEW, "review/b.jpg", null, null);
    linked.link();
    given(fileRepository.findAllById(any())).willReturn(List.of(pending, linked));

    fileService.markLinked(List.of(pending.getId(), linked.getId()));

    assertThat(pending.getStatus()).isEqualTo(FileStatus.LINKED);
    assertThat(linked.getStatus()).isEqualTo(FileStatus.LINKED);
  }

  @Test
  @DisplayName("존재하지 않는 fileId 로 조회하면 FILE_NOT_FOUND 예외가 발생한다")
  void getById_없는파일_예외발생() {
    UUID fileId = UUID.randomUUID();
    given(fileRepository.findById(fileId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> fileService.getById(fileId))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.FILE_NOT_FOUND);
  }

  @Test
  @DisplayName("존재하지 않는 fileId 가 섞여 있으면 markLinked 는 FILE_NOT_FOUND 예외가 발생한다")
  void markLinked_없는파일포함_예외발생() {
    File pending = File.createPending(UPLOADER_ID, FileDomain.REVIEW, "review/a.jpg", null, null);
    given(fileRepository.findAllById(any())).willReturn(List.of(pending));
    List<UUID> fileIds = List.of(pending.getId(), UUID.randomUUID());

    assertThatThrownBy(() -> fileService.markLinked(fileIds))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.FILE_NOT_FOUND);
  }

  @Test
  @DisplayName("48시간 지난 PENDING 파일 중 S3 삭제에 성공한 것만 DB 에서 삭제한다")
  void deleteStalePendingFiles_일부S3삭제실패_성공한것만DB삭제() {
    File deletable = File.createPending(UPLOADER_ID, FileDomain.DIARY, "diary/a.jpg", null, null);
    File failed = File.createPending(UPLOADER_ID, FileDomain.DIARY, "diary/b.jpg", null, null);
    given(
            fileRepository.findAllByStatusAndCreatedAtBefore(
                FileStatus.PENDING, NOW.minus(FileService.PENDING_RETENTION)))
        .willReturn(List.of(deletable, failed));
    given(fileStorage.deleteAll(List.of("diary/a.jpg", "diary/b.jpg")))
        .willReturn(Set.of("diary/a.jpg"));

    FileCleanupResult result = fileService.deleteStalePendingFiles();

    assertThat(result).isEqualTo(new FileCleanupResult(2, 1));
    verify(fileRepository).deleteAllInBatch(List.of(deletable));
  }

  @Test
  @DisplayName("정리 대상이 없으면 S3 를 호출하지 않는다")
  void deleteStalePendingFiles_대상없음_S3호출안함() {
    given(fileRepository.findAllByStatusAndCreatedAtBefore(any(), any())).willReturn(List.of());

    FileCleanupResult result = fileService.deleteStalePendingFiles();

    assertThat(result).isEqualTo(new FileCleanupResult(0, 0));
    verify(fileStorage, never()).deleteAll(any());
  }

  @Test
  @DisplayName("DIARY 도메인은 20MB 이하 PDF 를 받고 key 확장자를 pdf 로 만든다")
  void createPresignedUpload_다이어리PDF_허용() throws Exception {
    given(fileRepository.save(any(File.class))).willAnswer(invocation -> invocation.getArgument(0));
    given(fileStorage.presignPut(anyString(), anyString(), anyLong(), any()))
        .willReturn(URI.create("https://example.com/signed").toURL());

    PresignedUploadResponse response =
        fileService.createPresignedUpload(
            UPLOADER_ID,
            new PresignedUploadRequest(
                "DIARY", FileService.PDF_CONTENT_TYPE, FileService.MAX_PDF_SIZE_BYTES));

    assertThat(response.s3Key()).startsWith("diary/2026/10/").endsWith(".pdf");
  }

  @Test
  @DisplayName("DIARY 가 아닌 도메인에 PDF 를 올리면 FILE_INVALID_CONTENT_TYPE 예외가 발생한다")
  void createPresignedUpload_다이어리외PDF_예외발생() {
    PresignedUploadRequest request =
        new PresignedUploadRequest("REVIEW", FileService.PDF_CONTENT_TYPE, 1024L);

    assertThatThrownBy(() -> fileService.createPresignedUpload(UPLOADER_ID, request))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.FILE_INVALID_CONTENT_TYPE);
  }

  @Test
  @DisplayName("PDF 가 20MB 를 넘으면 FILE_TOO_LARGE 예외가 발생한다")
  void createPresignedUpload_PDF20MB초과_예외발생() {
    PresignedUploadRequest request =
        new PresignedUploadRequest(
            "DIARY", FileService.PDF_CONTENT_TYPE, FileService.MAX_PDF_SIZE_BYTES + 1);

    assertThatThrownBy(() -> fileService.createPresignedUpload(UPLOADER_ID, request))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.FILE_TOO_LARGE);
  }

  @Test
  @DisplayName("DIARY 도메인이어도 이미지는 10MB 제한을 유지한다")
  void createPresignedUpload_다이어리이미지10MB초과_예외발생() {
    PresignedUploadRequest request =
        new PresignedUploadRequest("DIARY", "image/png", FileService.MAX_SIZE_BYTES + 1);

    assertThatThrownBy(() -> fileService.createPresignedUpload(UPLOADER_ID, request))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(ErrorCode.FILE_TOO_LARGE);
  }
}
