package com.seoulaim.domain.diary.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seoulaim.domain.diary.admin.dto.AdminDiarySummaryResponse;
import com.seoulaim.domain.diary.dto.DiaryDetailResponse;
import com.seoulaim.domain.diary.dto.DiaryDownloadResponse;
import com.seoulaim.domain.diary.dto.DiaryPendingOrderResponse;
import com.seoulaim.domain.diary.dto.DiarySubmitRequest;
import com.seoulaim.domain.diary.dto.DiarySubmitResponse;
import com.seoulaim.domain.diary.dto.DiarySummaryResponse;
import com.seoulaim.domain.diary.entity.Diary;
import com.seoulaim.domain.diary.repository.DiaryRepository;
import com.seoulaim.domain.file.dto.FileDetail;
import com.seoulaim.domain.file.entity.FileDomain;
import com.seoulaim.domain.file.service.FileService;
import com.seoulaim.domain.order.dto.OrderItemInfo;
import com.seoulaim.domain.order.service.OrderService;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryService {

  static final int MAX_CONTENT_BYTES = 1024 * 1024;
  private static final String DOWNLOAD_FILENAME_FORMAT = "seoulaim-diary-%s.pdf";

  private final DiaryRepository diaryRepository;
  private final OrderService orderService;
  private final FileService fileService;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  /** 결제 완료된 다이어리 주문 중 아직 제출하지 않은 것. */
  public List<DiaryPendingOrderResponse> getPendingOrders(UUID userId) {
    List<OrderItemInfo> items = orderService.getPaidDiaryItems(userId);
    if (items.isEmpty()) {
      return List.of();
    }
    Set<UUID> submitted =
        diaryRepository
            .findAllByOrderItemIdIn(items.stream().map(OrderItemInfo::orderItemId).toList())
            .stream()
            .map(Diary::getOrderItemId)
            .collect(Collectors.toSet());
    return items.stream()
        .filter(item -> !submitted.contains(item.orderItemId()))
        .map(DiaryPendingOrderResponse::from)
        .toList();
  }

  @Transactional
  public DiarySubmitResponse submit(UUID userId, DiarySubmitRequest request) {
    orderService
        .findPaidDiaryItem(userId, request.orderItemId())
        .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_ORDER_NOT_ELIGIBLE));
    if (diaryRepository.existsByOrderItemId(request.orderItemId())) {
      throw new BusinessException(ErrorCode.DIARY_ALREADY_SUBMITTED);
    }
    FileDetail pdf =
        fileService
            .findDetail(request.pdfFileId())
            .filter(file -> isSubmittablePdf(userId, file))
            .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_INVALID_PDF));
    validateContentSize(request.content());

    fileService.markLinked(List.of(pdf.fileId()));
    Diary diary =
        Diary.submit(
            userId,
            request.orderItemId(),
            request.pageCount(),
            request.content(),
            pdf.fileId(),
            pdf.s3Key(),
            clock.instant());
    try {
      // 동시 제출은 order_item_id unique 제약으로 막고 409 로 바꾼다
      diaryRepository.saveAndFlush(diary);
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(ErrorCode.DIARY_ALREADY_SUBMITTED);
    }
    return new DiarySubmitResponse(diary.getId());
  }

  public List<DiarySummaryResponse> getMyDiaries(UUID userId) {
    return withOrderItems(
        diaryRepository.findAllByUserIdOrderBySubmittedAtDescIdDesc(userId),
        DiarySummaryResponse::of);
  }

  public DiaryDetailResponse getMyDiary(UUID userId, UUID diaryId) {
    return DiaryDetailResponse.from(findMyDiary(userId, diaryId));
  }

  public DiaryDownloadResponse getDownloadUrl(UUID userId, UUID diaryId) {
    Diary diary = findMyDiary(userId, diaryId);
    OrderItemInfo item =
        orderService.getOrderItems(List.of(diary.getOrderItemId())).stream()
            .findFirst()
            .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));
    return DiaryDownloadResponse.from(
        fileService.createPresignedDownload(
            diary.getPdfS3Key(), DOWNLOAD_FILENAME_FORMAT.formatted(item.orderNumber())));
  }

  public List<AdminDiarySummaryResponse> getAdminDiaries() {
    return withOrderItems(
        diaryRepository.findTop200ByOrderBySubmittedAtDescIdDesc(), AdminDiarySummaryResponse::of);
  }

  private Diary findMyDiary(UUID userId, UUID diaryId) {
    return diaryRepository
        .findByIdAndUserId(diaryId, userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.DIARY_NOT_FOUND));
  }

  // 남이 올린 파일을 끌어와 제출하지 못하게 업로더도 확인한다
  private static boolean isSubmittablePdf(UUID userId, FileDetail file) {
    return file.domain() == FileDomain.DIARY
        && FileService.PDF_CONTENT_TYPE.equals(file.contentType())
        && userId.equals(file.uploaderId());
  }

  private void validateContentSize(Map<String, Object> content) {
    try {
      if (objectMapper.writeValueAsBytes(content).length > MAX_CONTENT_BYTES) {
        throw new BusinessException(ErrorCode.DIARY_CONTENT_TOO_LARGE);
      }
    } catch (JsonProcessingException exception) {
      throw new BusinessException(ErrorCode.COMMON_INVALID_REQUEST);
    }
  }

  private <T> List<T> withOrderItems(
      List<Diary> diaries, BiFunction<Diary, OrderItemInfo, T> mapper) {
    if (diaries.isEmpty()) {
      return List.of();
    }
    Map<UUID, OrderItemInfo> itemsById =
        orderService.getOrderItems(diaries.stream().map(Diary::getOrderItemId).toList()).stream()
            .collect(Collectors.toMap(OrderItemInfo::orderItemId, Function.identity()));
    return diaries.stream()
        .map(diary -> mapper.apply(diary, itemsById.get(diary.getOrderItemId())))
        .toList();
  }
}
