package com.seoulaim.domain.file.service;

import com.seoulaim.global.config.StorageProperties;
import java.net.URL;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Error;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/** S3 호환 스토리지 접근. 서비스 테스트에서 mock 으로 대체한다. */
@Component
@RequiredArgsConstructor
public class FileStorage {

  // S3 DeleteObjects 한 번에 지울 수 있는 최대 개수
  private static final int DELETE_BATCH_SIZE = 1000;

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final StorageProperties storageProperties;

  /** Content-Type 과 Content-Length 를 서명에 포함하므로 업로드 시 같은 값을 보내야 한다. */
  public URL presignPut(String key, String contentType, long sizeBytes, Duration expiration) {
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder()
            .bucket(storageProperties.bucket())
            .key(key)
            .contentType(contentType)
            .contentLength(sizeBytes)
            .build();
    PutObjectPresignRequest presignRequest =
        PutObjectPresignRequest.builder()
            .signatureDuration(expiration)
            .putObjectRequest(putObjectRequest)
            .build();
    return s3Presigner.presignPutObject(presignRequest).url();
  }

  /** 브라우저가 filename 으로 저장하도록 응답 Content-Disposition 을 서명에 넣는다. */
  public URL presignGet(String key, String filename, Duration expiration) {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder()
            .bucket(storageProperties.bucket())
            .key(key)
            .responseContentDisposition("attachment; filename=\"%s\"".formatted(filename))
            .build();
    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(expiration)
            .getObjectRequest(getObjectRequest)
            .build();
    return s3Presigner.presignGetObject(presignRequest).url();
  }

  /** 객체를 삭제하고 삭제에 성공한 key 를 반환한다. 존재하지 않는 key 도 성공으로 본다. */
  public Set<String> deleteAll(List<String> keys) {
    Set<String> deletedKeys = new HashSet<>();
    for (int from = 0; from < keys.size(); from += DELETE_BATCH_SIZE) {
      List<String> batch = keys.subList(from, Math.min(from + DELETE_BATCH_SIZE, keys.size()));
      List<ObjectIdentifier> objects =
          batch.stream().map(key -> ObjectIdentifier.builder().key(key).build()).toList();
      DeleteObjectsResponse response =
          s3Client.deleteObjects(
              request ->
                  request
                      .bucket(storageProperties.bucket())
                      .delete(Delete.builder().objects(objects).quiet(true).build()));

      Set<String> failedKeys = new HashSet<>();
      for (S3Error error : response.errors()) {
        failedKeys.add(error.key());
      }
      batch.stream().filter(key -> !failedKeys.contains(key)).forEach(deletedKeys::add);
    }
    return deletedKeys;
  }
}
