package com.seoulaim.domain.inquiry.service;

import com.seoulaim.domain.diary.dto.RefundRequestCheck;
import com.seoulaim.domain.diary.service.DiaryPaymentService;
import com.seoulaim.domain.file.dto.FileDetail;
import com.seoulaim.domain.file.entity.FileDomain;
import com.seoulaim.domain.file.service.FileService;
import com.seoulaim.domain.inquiry.dto.InquiryCreateRequest;
import com.seoulaim.domain.inquiry.dto.InquiryCreateResponse;
import com.seoulaim.domain.inquiry.entity.Inquiry;
import com.seoulaim.domain.inquiry.entity.InquiryCategory;
import com.seoulaim.domain.inquiry.entity.RefundStatus;
import com.seoulaim.domain.inquiry.repository.InquiryRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원 문의 작성 (MR03). 어드민 처리는 AdminInquiryService, 환불 결정은 RefundRequestService. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

  static final int MAX_TITLE_LENGTH = 50;
  static final int MAX_CONTENT_LENGTH = 500;
  static final int MAX_PHOTOS = 5;
  static final Set<String> LANGUAGES = Set.of("en", "ko", "zh", "ja");
  private static final List<RefundStatus> ACTIVE_REFUND_STATUSES =
      List.of(RefundStatus.REQUESTED, RefundStatus.REFUNDED);
  private static final String IMAGE_CONTENT_TYPE_PREFIX = "image/";

  private final InquiryRepository inquiryRepository;
  private final DiaryPaymentService diaryPaymentService;
  private final FileService fileService;

  /**
   * 문의를 접수한다. 환불 문의는 본인의 PAID 결제만 고를 수 있고, 같은 결제에 처리 중·환불된 요청이 있으면 409.
   *
   * <p>환불 정책 판정(refundWithinPolicy)은 접수 시점 기준이며, 정책 밖(7일 경과·편집·완성)이어도 접수는 받는다.
   */
  @Transactional
  public InquiryCreateResponse submit(UUID userId, InquiryCreateRequest request) {
    InquiryCategory category =
        InquiryCategory.parse(request.category())
            .orElseThrow(() -> invalid("category", "문의 유형이 올바르지 않습니다"));
    String title = requireText(request.title(), MAX_TITLE_LENGTH, "title");
    String content = requireText(request.content(), MAX_CONTENT_LENGTH, "content");
    String language = request.language() == null ? null : request.language().strip();
    if (language == null || !LANGUAGES.contains(language)) {
      throw invalid("language", "en, ko, zh, ja 중 하나여야 합니다");
    }
    List<UUID> photoFileIds = request.photoFileIds() == null ? List.of() : request.photoFileIds();
    List<String> photoKeys = loadPhotoKeys(userId, photoFileIds);

    Inquiry inquiry =
        category == InquiryCategory.REFUND
            ? createRefund(userId, request.paymentId(), title, content, photoKeys, language)
            : Inquiry.create(userId, category, title, content, photoKeys, language);
    try {
      // 같은 결제의 동시 환불 신청은 partial unique 인덱스로 막는다
      inquiryRepository.saveAndFlush(inquiry);
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(ErrorCode.INQUIRY_REFUND_ALREADY_REQUESTED);
    }
    fileService.markLinked(photoFileIds);
    return new InquiryCreateResponse(inquiry.getId());
  }

  private Inquiry createRefund(
      UUID userId,
      UUID paymentId,
      String title,
      String content,
      List<String> photoKeys,
      String language) {
    if (paymentId == null) {
      throw invalid("paymentId", "환불 문의는 결제를 골라야 합니다");
    }
    RefundRequestCheck check =
        diaryPaymentService
            .checkRefundRequest(userId, paymentId)
            .orElseThrow(() -> invalid("paymentId", "환불을 요청할 수 있는 결제가 아닙니다"));
    if (inquiryRepository.existsByPaymentIdAndRefundStatusIn(paymentId, ACTIVE_REFUND_STATUSES)) {
      throw new BusinessException(ErrorCode.INQUIRY_REFUND_ALREADY_REQUESTED);
    }
    return Inquiry.createRefund(
        userId, title, content, photoKeys, language, paymentId, check.withinPolicy());
  }

  // 본인이 INQUIRY 도메인으로 올린 이미지만 받는다. 순서대로 s3 key
  private List<String> loadPhotoKeys(UUID userId, List<UUID> fileIds) {
    if (fileIds.size() > MAX_PHOTOS) {
      throw invalid("photoFileIds", "사진은 최대 " + MAX_PHOTOS + "장입니다");
    }
    if (fileIds.stream().anyMatch(Objects::isNull)
        || new HashSet<>(fileIds).size() != fileIds.size()) {
      throw invalid("photoFileIds", "중복되거나 빈 파일 id 가 있습니다");
    }
    Map<UUID, FileDetail> files =
        fileService.findDetails(fileIds).stream()
            .collect(Collectors.toMap(FileDetail::fileId, Function.identity()));
    for (UUID fileId : fileIds) {
      FileDetail file = files.get(fileId);
      if (file == null
          || !userId.equals(file.uploaderId())
          || file.domain() != FileDomain.INQUIRY
          || file.contentType() == null
          || !file.contentType().startsWith(IMAGE_CONTENT_TYPE_PREFIX)) {
        throw invalid("photoFileIds", "사용할 수 없는 파일입니다: " + fileId);
      }
    }
    return fileIds.stream().map(id -> files.get(id).s3Key()).toList();
  }

  private static String requireText(String value, int maxLength, String field) {
    String trimmed = value == null ? "" : value.strip();
    if (trimmed.isEmpty() || trimmed.codePointCount(0, trimmed.length()) > maxLength) {
      throw invalid(field, "1~" + maxLength + "자여야 합니다");
    }
    return trimmed;
  }

  static BusinessException invalid(String field, String message) {
    return new BusinessException(ErrorCode.INQUIRY_INVALID, Map.of(field, message));
  }
}
