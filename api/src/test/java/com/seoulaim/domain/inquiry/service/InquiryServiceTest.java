package com.seoulaim.domain.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.diary.dto.RefundRequestCheck;
import com.seoulaim.domain.diary.service.DiaryPaymentService;
import com.seoulaim.domain.file.dto.FileDetail;
import com.seoulaim.domain.file.entity.FileDomain;
import com.seoulaim.domain.file.entity.FileStatus;
import com.seoulaim.domain.file.service.FileService;
import com.seoulaim.domain.inquiry.dto.InquiryCreateRequest;
import com.seoulaim.domain.inquiry.dto.InquiryCreateResponse;
import com.seoulaim.domain.inquiry.entity.Inquiry;
import com.seoulaim.domain.inquiry.entity.InquiryCategory;
import com.seoulaim.domain.inquiry.entity.InquiryStatus;
import com.seoulaim.domain.inquiry.entity.RefundStatus;
import com.seoulaim.domain.inquiry.repository.InquiryRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();

  @Mock private InquiryRepository inquiryRepository;
  @Mock private DiaryPaymentService diaryPaymentService;
  @Mock private FileService fileService;
  @InjectMocks private InquiryService inquiryService;

  private static InquiryCreateRequest request(String category, List<UUID> photos, UUID paymentId) {
    return new InquiryCreateRequest(category, "  Title  ", " Content ", photos, "ko", paymentId);
  }

  private static FileDetail photo(UUID uploaderId, FileDomain domain) {
    UUID id = UUID.randomUUID();
    return new FileDetail(
        id, uploaderId, domain, "inquiry/2026/11/" + id + ".jpg", "image/jpeg", FileStatus.PENDING);
  }

  private Inquiry savedInquiry() {
    ArgumentCaptor<Inquiry> captor = ArgumentCaptor.forClass(Inquiry.class);
    verify(inquiryRepository).saveAndFlush(captor.capture());
    return captor.getValue();
  }

  @Test
  @DisplayName("일반 문의는 제목·내용을 다듬어 RECEIVED 로 저장하고 사진을 LINKED 처리한다")
  void submit_일반문의_저장() {
    FileDetail file = photo(USER_ID, FileDomain.INQUIRY);
    given(fileService.findDetails(List.of(file.fileId()))).willReturn(List.of(file));

    InquiryCreateResponse response =
        inquiryService.submit(
            USER_ID, request("PAYMENT", List.of(file.fileId()), UUID.randomUUID()));

    Inquiry saved = savedInquiry();
    assertThat(response.id()).isEqualTo(saved.getId());
    assertThat(saved.getCategory()).isEqualTo(InquiryCategory.PAYMENT);
    assertThat(saved.getTitle()).isEqualTo("Title");
    assertThat(saved.getContent()).isEqualTo("Content");
    assertThat(saved.getPhotoKeys()).containsExactly(file.s3Key());
    assertThat(saved.getStatus()).isEqualTo(InquiryStatus.RECEIVED);
    // 환불이 아닌 문의의 paymentId 는 무시한다
    assertThat(saved.getPaymentId()).isNull();
    assertThat(saved.getRefundStatus()).isNull();
    verify(fileService).markLinked(List.of(file.fileId()));
  }

  @Test
  @DisplayName("환불 문의는 결제의 접수 시점 정책 판정을 저장하고 REQUESTED 가 된다 (정책 밖이어도 접수)")
  void submit_환불문의_판정저장() {
    UUID paymentId = UUID.randomUUID();
    given(diaryPaymentService.checkRefundRequest(USER_ID, paymentId))
        .willReturn(Optional.of(new RefundRequestCheck(paymentId, false)));

    inquiryService.submit(USER_ID, request("refund", null, paymentId));

    Inquiry saved = savedInquiry();
    assertThat(saved.getCategory()).isEqualTo(InquiryCategory.REFUND);
    assertThat(saved.getPaymentId()).isEqualTo(paymentId);
    assertThat(saved.getRefundWithinPolicy()).isFalse();
    assertThat(saved.getRefundStatus()).isEqualTo(RefundStatus.REQUESTED);
  }

  @Test
  @DisplayName("같은 결제에 처리 중·환불된 요청이 있으면 409 INQUIRY_REFUND_ALREADY_REQUESTED")
  void submit_중복환불신청_409() {
    UUID paymentId = UUID.randomUUID();
    given(diaryPaymentService.checkRefundRequest(USER_ID, paymentId))
        .willReturn(Optional.of(new RefundRequestCheck(paymentId, true)));
    given(
            inquiryRepository.existsByPaymentIdAndRefundStatusIn(
                paymentId, List.of(RefundStatus.REQUESTED, RefundStatus.REFUNDED)))
        .willReturn(true);

    assertThatThrownBy(() -> inquiryService.submit(USER_ID, request("refund", null, paymentId)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INQUIRY_REFUND_ALREADY_REQUESTED);
    verify(inquiryRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("동시 신청이 unique 인덱스에 걸리면 409 INQUIRY_REFUND_ALREADY_REQUESTED")
  void submit_동시환불신청_409() {
    UUID paymentId = UUID.randomUUID();
    given(diaryPaymentService.checkRefundRequest(USER_ID, paymentId))
        .willReturn(Optional.of(new RefundRequestCheck(paymentId, true)));
    given(inquiryRepository.saveAndFlush(any()))
        .willThrow(new DataIntegrityViolationException("uk"));

    assertThatThrownBy(() -> inquiryService.submit(USER_ID, request("refund", null, paymentId)))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INQUIRY_REFUND_ALREADY_REQUESTED);
    verify(fileService, never()).markLinked(anyList());
  }

  @Test
  @DisplayName("환불 문의에 paymentId 가 없거나 본인의 PAID 결제가 아니면 400 INQUIRY_INVALID")
  void submit_환불결제부적격_400() {
    UUID paymentId = UUID.randomUUID();
    given(diaryPaymentService.checkRefundRequest(USER_ID, paymentId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> inquiryService.submit(USER_ID, request("refund", null, null)))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INQUIRY_INVALID);
    assertThatThrownBy(() -> inquiryService.submit(USER_ID, request("refund", null, paymentId)))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INQUIRY_INVALID);
  }

  @Test
  @DisplayName("유형·제목·내용·언어 검증 실패는 400 INQUIRY_INVALID")
  void submit_필드검증_400() {
    List<InquiryCreateRequest> invalid =
        List.of(
            new InquiryCreateRequest("bug", "t", "c", null, "en", null),
            new InquiryCreateRequest("other", "  ", "c", null, "en", null),
            new InquiryCreateRequest("other", "a".repeat(51), "c", null, "en", null),
            new InquiryCreateRequest("other", "t", "a".repeat(501), null, "en", null),
            new InquiryCreateRequest("other", "t", "c", null, "fr", null),
            new InquiryCreateRequest("other", "t", "c", null, null, null));

    for (InquiryCreateRequest request : invalid) {
      assertThatThrownBy(() -> inquiryService.submit(USER_ID, request))
          .extracting("errorCode")
          .isEqualTo(ErrorCode.INQUIRY_INVALID);
    }
    verify(inquiryRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("사진이 5장을 넘거나, 남의 파일이거나, INQUIRY 도메인이 아니면 400 INQUIRY_INVALID")
  void submit_사진검증_400() {
    List<UUID> six = Stream.generate(UUID::randomUUID).limit(6).toList();
    FileDetail others = photo(UUID.randomUUID(), FileDomain.INQUIRY);
    FileDetail wrongDomain = photo(USER_ID, FileDomain.DIARY);
    given(fileService.findDetails(List.of(others.fileId()))).willReturn(List.of(others));
    given(fileService.findDetails(List.of(wrongDomain.fileId()))).willReturn(List.of(wrongDomain));

    assertThatThrownBy(() -> inquiryService.submit(USER_ID, request("other", six, null)))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INQUIRY_INVALID);
    assertThatThrownBy(
            () -> inquiryService.submit(USER_ID, request("other", List.of(others.fileId()), null)))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INQUIRY_INVALID);
    assertThatThrownBy(
            () ->
                inquiryService.submit(
                    USER_ID, request("other", List.of(wrongDomain.fileId()), null)))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INQUIRY_INVALID);
    verify(fileService, never()).markLinked(anyList());
  }
}
