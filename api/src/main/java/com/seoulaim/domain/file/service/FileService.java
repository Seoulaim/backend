package com.seoulaim.domain.file.service;

import com.github.f4b6a3.uuid.UuidCreator;
import com.seoulaim.domain.file.dto.FileCleanupResult;
import com.seoulaim.domain.file.dto.FileResponse;
import com.seoulaim.domain.file.dto.PresignedUploadRequest;
import com.seoulaim.domain.file.dto.PresignedUploadResponse;
import com.seoulaim.domain.file.entity.File;
import com.seoulaim.domain.file.entity.FileDomain;
import com.seoulaim.domain.file.entity.FileStatus;
import com.seoulaim.domain.file.repository.FileRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.net.URL;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {

  static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
  static final Duration UPLOAD_URL_EXPIRATION = Duration.ofMinutes(10);
  static final Duration PENDING_RETENTION = Duration.ofHours(48);

  // 허용 Content-Type → s3 key 확장자
  private static final Map<String, String> EXTENSIONS =
      Map.of(
          "image/jpeg", "jpg",
          "image/png", "png",
          "image/webp", "webp",
          "image/heic", "heic");
  private static final DateTimeFormatter KEY_MONTH_FORMAT =
      DateTimeFormatter.ofPattern("yyyy/MM").withZone(ZoneOffset.UTC);

  private final FileRepository fileRepository;
  private final FileStorage fileStorage;
  private final Clock clock;

  @Transactional
  public PresignedUploadResponse createPresignedUpload(
      UUID uploaderId, PresignedUploadRequest request) {
    FileDomain domain = FileDomain.from(request.domain());
    String extension = EXTENSIONS.get(request.contentType());
    if (extension == null) {
      throw new BusinessException(ErrorCode.FILE_INVALID_CONTENT_TYPE);
    }
    if (request.sizeBytes() > MAX_SIZE_BYTES) {
      throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
    }

    Instant now = clock.instant();
    String s3Key = generateKey(domain, now, extension);
    File file =
        fileRepository.save(
            File.createPending(
                uploaderId, domain, s3Key, request.contentType(), request.sizeBytes()));
    URL uploadUrl =
        fileStorage.presignPut(
            s3Key, request.contentType(), request.sizeBytes(), UPLOAD_URL_EXPIRATION);

    return new PresignedUploadResponse(
        file.getId(), s3Key, uploadUrl.toString(), now.plus(UPLOAD_URL_EXPIRATION));
  }

  public FileResponse getById(UUID fileId) {
    return fileRepository
        .findById(fileId)
        .map(FileResponse::from)
        .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
  }

  /** s3 key 로 파일을 조회한다. 없는 key 는 결과에서 빠진다. */
  public List<FileResponse> getByS3Keys(Collection<String> s3Keys) {
    if (s3Keys.isEmpty()) {
      return List.of();
    }
    return fileRepository.findAllByS3KeyIn(s3Keys).stream().map(FileResponse::from).toList();
  }

  /**
   * 다른 도메인이 파일을 엔티티에 연결할 때 호출한다. 이미 LINKED 인 파일은 그대로 둔다.
   *
   * @throws BusinessException 존재하지 않는 fileId 가 섞여 있으면 FILE_NOT_FOUND
   */
  @Transactional
  public void markLinked(List<UUID> fileIds) {
    if (fileIds.isEmpty()) {
      return;
    }
    Set<UUID> uniqueIds = new HashSet<>(fileIds);
    List<File> files = fileRepository.findAllById(uniqueIds);
    if (files.size() != uniqueIds.size()) {
      throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
    }
    files.stream().filter(file -> !file.isLinked()).forEach(File::link);
  }

  /** 48시간 넘게 PENDING 인 파일을 S3 에서 지운 뒤 DB 에서 삭제한다. */
  @Transactional
  public FileCleanupResult deleteStalePendingFiles() {
    Instant threshold = clock.instant().minus(PENDING_RETENTION);
    List<File> staleFiles =
        fileRepository.findAllByStatusAndCreatedAtBefore(FileStatus.PENDING, threshold);
    if (staleFiles.isEmpty()) {
      return new FileCleanupResult(0, 0);
    }

    Set<String> deletedKeys =
        fileStorage.deleteAll(staleFiles.stream().map(File::getS3Key).toList());
    List<File> deletable =
        staleFiles.stream().filter(file -> deletedKeys.contains(file.getS3Key())).toList();
    fileRepository.deleteAllInBatch(deletable);

    return new FileCleanupResult(staleFiles.size(), deletable.size());
  }

  private String generateKey(FileDomain domain, Instant now, String extension) {
    return "%s/%s/%s.%s"
        .formatted(
            domain.keyPrefix(),
            KEY_MONTH_FORMAT.format(now),
            UuidCreator.getTimeOrderedEpoch(),
            extension);
  }
}
