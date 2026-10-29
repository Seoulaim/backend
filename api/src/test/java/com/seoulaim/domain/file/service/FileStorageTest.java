package com.seoulaim.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.seoulaim.global.config.StorageProperties;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Error;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class FileStorageTest {

  private static final String BUCKET = "seoulaim-test";

  @Mock private S3Client s3Client;
  @Mock private S3Presigner s3Presigner;
  @Mock private PresignedPutObjectRequest presignedRequest;

  private FileStorage fileStorage;

  @BeforeEach
  void setUp() {
    StorageProperties properties =
        new StorageProperties(null, "ap-northeast-2", BUCKET, null, null, false);
    fileStorage = new FileStorage(s3Client, s3Presigner, properties);
  }

  @Test
  @DisplayName("presignPut 은 버킷·key·Content-Type·Content-Length·만료시간을 서명 요청에 담는다")
  void presignPut_정상요청_서명요청에값포함() throws Exception {
    given(presignedRequest.url())
        .willReturn(URI.create("https://" + BUCKET + ".s3.amazonaws.com/signed").toURL());
    given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
        .willReturn(presignedRequest);

    fileStorage.presignPut("review/2026/10/a.jpg", "image/jpeg", 1024L, Duration.ofMinutes(10));

    ArgumentCaptor<PutObjectPresignRequest> captor =
        ArgumentCaptor.forClass(PutObjectPresignRequest.class);
    verify(s3Presigner).presignPutObject(captor.capture());
    PutObjectPresignRequest request = captor.getValue();
    assertThat(request.signatureDuration()).isEqualTo(Duration.ofMinutes(10));
    assertThat(request.putObjectRequest().bucket()).isEqualTo(BUCKET);
    assertThat(request.putObjectRequest().key()).isEqualTo("review/2026/10/a.jpg");
    assertThat(request.putObjectRequest().contentType()).isEqualTo("image/jpeg");
    assertThat(request.putObjectRequest().contentLength()).isEqualTo(1024L);
  }

  @Test
  @DisplayName("deleteAll 은 S3 가 에러로 돌려준 key 를 제외한 나머지만 삭제 성공으로 반환한다")
  @SuppressWarnings("unchecked")
  void deleteAll_일부실패_실패key제외반환() {
    given(s3Client.deleteObjects(any(Consumer.class)))
        .willReturn(
            DeleteObjectsResponse.builder()
                .errors(S3Error.builder().key("b.jpg").code("AccessDenied").build())
                .build());

    Set<String> deleted = fileStorage.deleteAll(List.of("a.jpg", "b.jpg", "c.jpg"));

    assertThat(deleted).containsExactlyInAnyOrder("a.jpg", "c.jpg");
  }

  @Test
  @DisplayName("deleteAll 은 1000개 단위로 나눠 DeleteObjects 를 호출한다")
  @SuppressWarnings("unchecked")
  void deleteAll_1001개_두번호출() {
    given(s3Client.deleteObjects(any(Consumer.class)))
        .willReturn(DeleteObjectsResponse.builder().build());
    List<String> keys = IntStream.range(0, 1001).mapToObj(i -> "k" + i).toList();

    Set<String> deleted = fileStorage.deleteAll(keys);

    assertThat(deleted).hasSize(1001);
    ArgumentCaptor<Consumer<DeleteObjectsRequest.Builder>> captor =
        ArgumentCaptor.forClass(Consumer.class);
    verify(s3Client, times(2)).deleteObjects(captor.capture());
    DeleteObjectsRequest.Builder first = DeleteObjectsRequest.builder();
    captor.getAllValues().get(0).accept(first);
    assertThat(first.build().bucket()).isEqualTo(BUCKET);
    assertThat(first.build().delete().objects()).hasSize(1000);
  }

  @Test
  @DisplayName("deleteAll 에 빈 목록을 주면 S3 를 호출하지 않는다")
  @SuppressWarnings("unchecked")
  void deleteAll_빈목록_호출안함() {
    assertThat(fileStorage.deleteAll(List.of())).isEmpty();
    verify(s3Client, never()).deleteObjects(any(Consumer.class));
  }

  @Test
  @DisplayName("presignGet 은 버킷·key·첨부 파일명·만료시간을 서명 요청에 담는다")
  void presignGet_정상요청_서명요청에값포함() throws Exception {
    PresignedGetObjectRequest presignedGet = mock(PresignedGetObjectRequest.class);
    given(presignedGet.url())
        .willReturn(URI.create("https://" + BUCKET + ".s3.amazonaws.com/signed").toURL());
    given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
        .willReturn(presignedGet);

    fileStorage.presignGet(
        "diary/2026/10/a.pdf", "seoulaim-diary-ORD-1.pdf", Duration.ofMinutes(10));

    ArgumentCaptor<GetObjectPresignRequest> captor =
        ArgumentCaptor.forClass(GetObjectPresignRequest.class);
    verify(s3Presigner).presignGetObject(captor.capture());
    GetObjectPresignRequest request = captor.getValue();
    assertThat(request.signatureDuration()).isEqualTo(Duration.ofMinutes(10));
    assertThat(request.getObjectRequest().bucket()).isEqualTo(BUCKET);
    assertThat(request.getObjectRequest().key()).isEqualTo("diary/2026/10/a.pdf");
    assertThat(request.getObjectRequest().responseContentDisposition())
        .isEqualTo("attachment; filename=\"seoulaim-diary-ORD-1.pdf\"");
  }
}
