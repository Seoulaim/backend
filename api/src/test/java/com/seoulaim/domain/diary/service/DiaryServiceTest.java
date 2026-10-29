package com.seoulaim.domain.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulaim.domain.diary.dto.DiaryDownloadResponse;
import com.seoulaim.domain.diary.dto.DiaryPendingOrderResponse;
import com.seoulaim.domain.diary.dto.DiarySubmitRequest;
import com.seoulaim.domain.diary.dto.DiarySubmitResponse;
import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.repository.DiaryRepository;
import com.seoulaim.domain.file.dto.FileDetail;
import com.seoulaim.domain.file.dto.PresignedDownloadResponse;
import com.seoulaim.domain.file.entity.FileDomain;
import com.seoulaim.domain.file.entity.FileStatus;
import com.seoulaim.domain.file.service.FileService;
import com.seoulaim.domain.order.dto.OrderItemInfo;
import com.seoulaim.domain.order.service.OrderService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
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
class DiaryServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-29T03:00:00Z");
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID ORDER_ITEM_ID = UUID.randomUUID();
  private static final UUID PDF_FILE_ID = UUID.randomUUID();
  private static final String PDF_KEY = "diary/2026/10/a.pdf";
  private static final Map<String, Object> CONTENT =
      Map.of("pages", List.of(Map.of("templateId", "t1", "elements", List.of())));
  private static final OrderItemInfo PAID_DIARY_ITEM =
      new OrderItemInfo(
          ORDER_ITEM_ID,
          UUID.randomUUID(),
          "ORD-20261029-AB12CD",
          UUID.randomUUID(),
          "Diary",
          NOW.minusSeconds(60));

  @Mock private DiaryRepository diaryRepository;
  @Mock private OrderService orderService;
  @Mock private FileService fileService;

  private DiaryService diaryService;

  @BeforeEach
  void setUp() {
    diaryService =
        new DiaryService(
            diaryRepository,
            orderService,
            fileService,
            new ObjectMapper(),
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static DiarySubmitRequest request(Map<String, Object> content) {
    return new DiarySubmitRequest(ORDER_ITEM_ID, 12, content, PDF_FILE_ID);
  }

  private static FileDetail file(FileDomain domain, String contentType, UUID uploaderId) {
    return new FileDetail(
        PDF_FILE_ID, uploaderId, domain, PDF_KEY, contentType, FileStatus.PENDING);
  }

  private static void assertErrorCode(Runnable call, ErrorCode errorCode) {
    assertThatThrownBy(call::run)
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).getErrorCode())
        .isEqualTo(errorCode);
  }

  @Test
  @DisplayName("결제 완료된 내 다이어리 주문이 아니면 DIARY_ORDER_NOT_ELIGIBLE 예외가 발생한다")
  void submit_미결제주문_예외발생() {
    given(orderService.findPaidDiaryItem(USER_ID, ORDER_ITEM_ID)).willReturn(Optional.empty());

    assertErrorCode(
        () -> diaryService.submit(USER_ID, request(CONTENT)), ErrorCode.DIARY_ORDER_NOT_ELIGIBLE);
    verify(fileService, never()).markLinked(any());
    verify(diaryRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("이미 제출한 주문 상품이면 DIARY_ALREADY_SUBMITTED 예외가 발생한다")
  void submit_중복제출_409예외발생() {
    given(orderService.findPaidDiaryItem(USER_ID, ORDER_ITEM_ID))
        .willReturn(Optional.of(PAID_DIARY_ITEM));
    given(diaryRepository.existsByOrderItemId(ORDER_ITEM_ID)).willReturn(true);

    assertErrorCode(
        () -> diaryService.submit(USER_ID, request(CONTENT)), ErrorCode.DIARY_ALREADY_SUBMITTED);
    verify(fileService, never()).markLinked(any());
  }

  @Test
  @DisplayName("PDF 가 아닌 파일이면 DIARY_INVALID_PDF 예외가 발생한다")
  void submit_PDF아닌파일_예외발생() {
    given(orderService.findPaidDiaryItem(USER_ID, ORDER_ITEM_ID))
        .willReturn(Optional.of(PAID_DIARY_ITEM));
    given(fileService.findDetail(PDF_FILE_ID))
        .willReturn(Optional.of(file(FileDomain.DIARY, "image/jpeg", USER_ID)));

    assertErrorCode(
        () -> diaryService.submit(USER_ID, request(CONTENT)), ErrorCode.DIARY_INVALID_PDF);
    verify(fileService, never()).markLinked(any());
  }

  @Test
  @DisplayName("다른 사용자가 올린 PDF 면 DIARY_INVALID_PDF 예외가 발생한다")
  void submit_타인업로드PDF_예외발생() {
    given(orderService.findPaidDiaryItem(USER_ID, ORDER_ITEM_ID))
        .willReturn(Optional.of(PAID_DIARY_ITEM));
    given(fileService.findDetail(PDF_FILE_ID))
        .willReturn(Optional.of(file(FileDomain.DIARY, "application/pdf", UUID.randomUUID())));

    assertErrorCode(
        () -> diaryService.submit(USER_ID, request(CONTENT)), ErrorCode.DIARY_INVALID_PDF);
  }

  @Test
  @DisplayName("편집 데이터가 1MB 를 넘으면 DIARY_CONTENT_TOO_LARGE 예외가 발생한다")
  void submit_content1MB초과_예외발생() {
    given(orderService.findPaidDiaryItem(USER_ID, ORDER_ITEM_ID))
        .willReturn(Optional.of(PAID_DIARY_ITEM));
    given(fileService.findDetail(PDF_FILE_ID))
        .willReturn(Optional.of(file(FileDomain.DIARY, "application/pdf", USER_ID)));
    Map<String, Object> large = Map.of("text", "a".repeat(DiaryService.MAX_CONTENT_BYTES));

    assertErrorCode(
        () -> diaryService.submit(USER_ID, request(large)), ErrorCode.DIARY_CONTENT_TOO_LARGE);
    verify(fileService, never()).markLinked(any());
  }

  @Test
  @DisplayName("정상 제출하면 PDF 를 LINKED 로 바꾸고 다이어리를 저장한다")
  void submit_정상요청_markLinked후저장() {
    given(orderService.findPaidDiaryItem(USER_ID, ORDER_ITEM_ID))
        .willReturn(Optional.of(PAID_DIARY_ITEM));
    given(fileService.findDetail(PDF_FILE_ID))
        .willReturn(Optional.of(file(FileDomain.DIARY, "application/pdf", USER_ID)));
    given(diaryRepository.saveAndFlush(any(Diary.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    DiarySubmitResponse response = diaryService.submit(USER_ID, request(CONTENT));

    verify(fileService).markLinked(List.of(PDF_FILE_ID));
    ArgumentCaptor<Diary> captor = ArgumentCaptor.forClass(Diary.class);
    verify(diaryRepository).saveAndFlush(captor.capture());
    Diary saved = captor.getValue();
    assertThat(response.diaryId()).isEqualTo(saved.getId());
    assertThat(saved.getUserId()).isEqualTo(USER_ID);
    assertThat(saved.getOrderItemId()).isEqualTo(ORDER_ITEM_ID);
    assertThat(saved.getPageCount()).isEqualTo(12);
    assertThat(saved.getPdfS3Key()).isEqualTo(PDF_KEY);
    assertThat(saved.getSubmittedAt()).isEqualTo(NOW);
  }

  @Test
  @DisplayName("제출 가능 주문 조회는 이미 제출한 주문 상품을 제외한다")
  void getPendingOrders_제출된주문포함_제외하고반환() {
    OrderItemInfo other =
        new OrderItemInfo(
            UUID.randomUUID(), UUID.randomUUID(), "ORD-20261029-ZZ99ZZ", null, "Diary", NOW);
    given(orderService.getPaidDiaryItems(USER_ID)).willReturn(List.of(PAID_DIARY_ITEM, other));
    given(diaryRepository.findAllByOrderItemIdIn(List.of(ORDER_ITEM_ID, other.orderItemId())))
        .willReturn(
            List.of(Diary.submit(USER_ID, ORDER_ITEM_ID, 1, CONTENT, PDF_FILE_ID, PDF_KEY, NOW)));

    List<DiaryPendingOrderResponse> pending = diaryService.getPendingOrders(USER_ID);

    assertThat(pending)
        .extracting(DiaryPendingOrderResponse::orderItemId)
        .containsExactly(other.orderItemId());
  }

  @Test
  @DisplayName("남의 다이어리 다운로드를 요청하면 DIARY_NOT_FOUND 예외가 발생한다")
  void getDownloadUrl_타인다이어리_예외발생() {
    UUID diaryId = UUID.randomUUID();
    given(diaryRepository.findByIdAndUserId(diaryId, USER_ID)).willReturn(Optional.empty());

    assertErrorCode(() -> diaryService.getDownloadUrl(USER_ID, diaryId), ErrorCode.DIARY_NOT_FOUND);
    verify(fileService, never()).createPresignedDownload(any(), any());
  }

  @Test
  @DisplayName("다운로드 URL 은 주문번호를 넣은 파일명으로 발급한다")
  void getDownloadUrl_정상요청_주문번호파일명() {
    Diary diary = Diary.submit(USER_ID, ORDER_ITEM_ID, 1, CONTENT, PDF_FILE_ID, PDF_KEY, NOW);
    given(diaryRepository.findByIdAndUserId(diary.getId(), USER_ID)).willReturn(Optional.of(diary));
    given(orderService.getOrderItems(List.of(ORDER_ITEM_ID))).willReturn(List.of(PAID_DIARY_ITEM));

    given(fileService.createPresignedDownload(PDF_KEY, "seoulaim-diary-ORD-20261029-AB12CD.pdf"))
        .willReturn(new PresignedDownloadResponse("https://example.com/signed", NOW));

    DiaryDownloadResponse response = diaryService.getDownloadUrl(USER_ID, diary.getId());

    assertThat(response.downloadUrl()).isEqualTo("https://example.com/signed");
    assertThat(response.expiresAt()).isEqualTo(NOW);
  }
}
