package com.seoulaim.domain.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.seoulaim.domain.inquiry.admin.dto.AdminInquiryAnswerRequest;
import com.seoulaim.domain.inquiry.admin.dto.AdminInquiryListResponse;
import com.seoulaim.domain.inquiry.admin.dto.AdminInquiryMemoRequest;
import com.seoulaim.domain.inquiry.admin.dto.AdminInquiryResponse;
import com.seoulaim.domain.inquiry.admin.dto.InquiryAuthor;
import com.seoulaim.domain.inquiry.entity.Inquiry;
import com.seoulaim.domain.inquiry.entity.InquiryAnswer;
import com.seoulaim.domain.inquiry.entity.InquiryCategory;
import com.seoulaim.domain.inquiry.entity.InquiryStatus;
import com.seoulaim.domain.inquiry.entity.RefundStatus;
import com.seoulaim.domain.inquiry.repository.InquiryAnswerRepository;
import com.seoulaim.domain.inquiry.repository.InquiryRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.mail.EmailSender;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminInquiryServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-05T03:00:00Z");
  private static final UUID USER_ID = UUID.randomUUID();

  @Mock private InquiryRepository inquiryRepository;
  @Mock private InquiryAnswerRepository inquiryAnswerRepository;
  @Mock private InquiryAuthorLoader inquiryAuthorLoader;
  @Mock private EmailSender emailSender;

  private AdminInquiryService adminInquiryService;

  @BeforeEach
  void setUp() {
    adminInquiryService =
        new AdminInquiryService(
            inquiryRepository,
            inquiryAnswerRepository,
            inquiryAuthorLoader,
            emailSender,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static Inquiry inquiry() {
    return Inquiry.create(USER_ID, InquiryCategory.OTHER, "Can I print?", "Body", List.of(), "en");
  }

  private Inquiry givenInquiry(String email) {
    Inquiry inquiry = inquiry();
    given(inquiryRepository.findByIdAndCategoryNot(inquiry.getId(), InquiryCategory.REFUND))
        .willReturn(Optional.of(inquiry));
    given(inquiryAuthorLoader.load(inquiry)).willReturn(new InquiryAuthor(USER_ID, "Hana", email));
    return inquiry;
  }

  @Test
  @DisplayName("목록은 환불 외 문의에 작성자·답변을 붙이고 처리 대기 환불 수를 함께 준다")
  void getInquiries_작성자답변_환불수() {
    Inquiry first = inquiry();
    Inquiry second = inquiry();
    given(inquiryRepository.findAllByCategoryNotOrderByCreatedAtDescIdDesc(InquiryCategory.REFUND))
        .willReturn(List.of(first, second));
    given(inquiryAuthorLoader.load(List.of(first, second)))
        .willReturn(Map.of(USER_ID, new InquiryAuthor(USER_ID, "Hana", "hana@example.com")));
    given(inquiryAnswerRepository.findAllByInquiryIdInOrderBySentAtAscIdAsc(any()))
        .willReturn(List.of(InquiryAnswer.create(second.getId(), "Hi", NOW)));
    given(inquiryRepository.countByRefundStatus(RefundStatus.REQUESTED)).willReturn(2L);

    AdminInquiryListResponse response = adminInquiryService.getInquiries();

    assertThat(response.refundCount()).isEqualTo(2);
    assertThat(response.inquiries()).hasSize(2);
    assertThat(response.inquiries().get(0).answers()).isEmpty();
    assertThat(response.inquiries().get(1).answers())
        .singleElement()
        .satisfies(
            answer -> {
              assertThat(answer.body()).isEqualTo("Hi");
              assertThat(answer.adminName()).isEqualTo("운영자");
            });
    assertThat(response.inquiries().get(0).author().email()).isEqualTo("hana@example.com");
  }

  @Test
  @DisplayName("답변은 작성자 이메일로 \"[Seoulaim] Re: 제목\" 메일을 보내고 기록한 뒤 ANSWERED 로 바꾼다")
  void answer_메일발송후_answered() {
    Inquiry inquiry = givenInquiry("hana@example.com");

    adminInquiryService.answer(inquiry.getId(), new AdminInquiryAnswerRequest("  Hi Hana  "));

    verify(emailSender).send("hana@example.com", "[Seoulaim] Re: Can I print?", "Hi Hana");
    ArgumentCaptor<InquiryAnswer> captor = ArgumentCaptor.forClass(InquiryAnswer.class);
    verify(inquiryAnswerRepository).save(captor.capture());
    assertThat(captor.getValue().getBody()).isEqualTo("Hi Hana");
    assertThat(captor.getValue().getSentAt()).isEqualTo(NOW);
    assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
  }

  @Test
  @DisplayName("메일 발송이 실패하면 답변을 저장하지 않고 상태도 그대로다")
  void answer_메일실패_저장안함() {
    Inquiry inquiry = givenInquiry("hana@example.com");
    willThrow(new BusinessException(ErrorCode.AUTH_MAIL_SEND_FAILED))
        .given(emailSender)
        .send(anyString(), anyString(), anyString());

    assertThatThrownBy(
            () -> adminInquiryService.answer(inquiry.getId(), new AdminInquiryAnswerRequest("Hi")))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AUTH_MAIL_SEND_FAILED);
    verify(inquiryAnswerRepository, never()).save(any());
    assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.RECEIVED);
  }

  @Test
  @DisplayName("탈퇴해 이메일이 없는 작성자에게는 답변할 수 없다 (400 INQUIRY_INVALID)")
  void answer_이메일없음_400() {
    Inquiry inquiry = givenInquiry(null);

    assertThatThrownBy(
            () -> adminInquiryService.answer(inquiry.getId(), new AdminInquiryAnswerRequest("Hi")))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INQUIRY_INVALID);
    verify(emailSender, never()).send(anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("환불 문의나 없는 id 는 404 INQUIRY_NOT_FOUND")
  void getInquiry_환불문의_404() {
    UUID id = UUID.randomUUID();
    given(inquiryRepository.findByIdAndCategoryNot(id, InquiryCategory.REFUND))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> adminInquiryService.getInquiry(id))
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INQUIRY_NOT_FOUND);
  }

  @Test
  @DisplayName("메모는 저장되고, 빈 문자열이면 지워진다")
  void saveMemo_저장과삭제() {
    Inquiry inquiry = givenInquiry("hana@example.com");

    AdminInquiryResponse saved =
        adminInquiryService.saveMemo(inquiry.getId(), new AdminInquiryMemoRequest("토스 확인"));
    assertThat(saved.memo()).isEqualTo("토스 확인");

    adminInquiryService.saveMemo(inquiry.getId(), new AdminInquiryMemoRequest("  "));
    assertThat(inquiry.getMemo()).isNull();
  }
}
