package com.seoulaim.domain.inquiry.service;

import com.seoulaim.domain.diary.dto.PaymentRefundInfo;
import com.seoulaim.domain.diary.service.DiaryPaymentService;
import com.seoulaim.domain.inquiry.admin.dto.AdminRefundApproveRequest;
import com.seoulaim.domain.inquiry.admin.dto.AdminRefundRejectRequest;
import com.seoulaim.domain.inquiry.admin.dto.AdminRefundRequestResponse;
import com.seoulaim.domain.inquiry.admin.dto.InquiryAuthor;
import com.seoulaim.domain.inquiry.entity.Inquiry;
import com.seoulaim.domain.inquiry.entity.InquiryCategory;
import com.seoulaim.domain.inquiry.repository.InquiryRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.mail.EmailSender;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 어드민 환불 관리. 환불은 REFUND 문의로만 들어오고, 승인하면 토스 결제를 취소한다.
 *
 * <p>결제·다이어리 변경은 DiaryPaymentService 경유 (diary 저장소를 직접 쓰지 않는다).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundRequestService {

  static final int MAX_EXCEPTION_REASON_LENGTH = 50;
  static final int MAX_EXCEPTION_MEMO_LENGTH = 2000;
  static final int MAX_REJECT_REASON_LENGTH = 1000;
  static final String OTHER_EXCEPTION_REASON = "other";
  static final String CANCEL_REASON = "고객 요청 환불";

  private final InquiryRepository inquiryRepository;
  private final DiaryPaymentService diaryPaymentService;
  private final InquiryAuthorLoader inquiryAuthorLoader;
  private final EmailSender emailSender;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  /** 처리 대기(REQUESTED) 먼저 오래된 순(기한 임박), 그다음 결정된 것 최신순. */
  public List<AdminRefundRequestResponse> getRefundRequests() {
    List<Inquiry> inquiries =
        inquiryRepository.findAllByCategoryOrderByCreatedAtAscIdAsc(InquiryCategory.REFUND);
    Map<UUID, InquiryAuthor> authors = inquiryAuthorLoader.load(inquiries);
    Map<UUID, PaymentRefundInfo> payments =
        diaryPaymentService
            .findRefundInfos(inquiries.stream().map(Inquiry::getPaymentId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(PaymentRefundInfo::id, Function.identity()));
    Stream<Inquiry> waiting = inquiries.stream().filter(Inquiry::isRefundRequested);
    Stream<Inquiry> decided =
        inquiries.stream()
            .filter(inquiry -> !inquiry.isRefundRequested())
            .sorted(
                Comparator.comparing(
                        Inquiry::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Inquiry::getId, Comparator.reverseOrder()));
    return Stream.concat(waiting, decided)
        .map(
            inquiry ->
                view(
                    inquiry,
                    authors.get(inquiry.getUserId()),
                    payments.get(inquiry.getPaymentId())))
        .toList();
  }

  public AdminRefundRequestResponse getRefundRequest(UUID inquiryId) {
    Inquiry inquiry =
        inquiryRepository
            .findByIdAndCategory(inquiryId, InquiryCategory.REFUND)
            .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));
    return view(inquiry);
  }

  /**
   * 승인: (정책 밖이면 예외 사유 필수) → 토스 전액 취소 → 결제 REFUNDED·다이어리 처리 → 문의 REFUNDED·ANSWERED → 커밋 후 "환불 완료"
   * 메일.
   *
   * <p>토스 취소가 실패하면 예외로 트랜잭션이 롤백되어 결제·다이어리·문의 모두 그대로다.
   */
  @Transactional
  public AdminRefundRequestResponse approve(UUID inquiryId, AdminRefundApproveRequest request) {
    Inquiry inquiry = lockPendingRefund(inquiryId);
    AdminRefundApproveRequest.PolicyException exception =
        normalizeException(request == null ? null : request.exception());
    if (!inquiry.isRefundWithinPolicy() && exception == null) {
      throw new BusinessException(ErrorCode.INQUIRY_POLICY_EXCEPTION_REQUIRED);
    }

    PaymentRefundInfo payment = diaryPaymentService.refund(inquiry.getPaymentId(), CANCEL_REASON);
    inquiry.approveRefund(
        exception == null ? null : exception.reason(),
        exception == null ? null : exception.memo(),
        clock.instant());

    InquiryAuthor author = inquiryAuthorLoader.load(inquiry);
    if (author.email() == null) {
      log.warn("환불 완료 메일 생략: 작성자 이메일 없음 inquiryId={}", inquiry.getId());
    } else {
      RefundMails.Mail mail =
          RefundMails.approved(
              inquiry.getLanguage(),
              inquiry.getTitle(),
              payment.orderNumber(),
              payment.amount(),
              payment.diary() != null && payment.diary().first());
      eventPublisher.publishEvent(
          new RefundApprovedEvent(author.email(), mail.subject(), mail.text()));
    }
    return view(inquiry, author, payment);
  }

  /** 거절: REJECTED·ANSWERED 로 바꾸고 사유를 메일로 보낸다. 메일 발송이 실패하면 롤백된다(503). */
  @Transactional
  public AdminRefundRequestResponse reject(UUID inquiryId, AdminRefundRejectRequest request) {
    Inquiry inquiry = lockPendingRefund(inquiryId);
    String reason = request == null || request.reason() == null ? "" : request.reason().strip();
    if (reason.isEmpty() || reason.codePointCount(0, reason.length()) > MAX_REJECT_REASON_LENGTH) {
      throw InquiryService.invalid("reason", "1~" + MAX_REJECT_REASON_LENGTH + "자여야 합니다");
    }
    inquiry.rejectRefund(reason, clock.instant());

    InquiryAuthor author = inquiryAuthorLoader.load(inquiry);
    if (author.email() == null) {
      log.warn("환불 거절 메일 생략: 작성자 이메일 없음 inquiryId={}", inquiry.getId());
    } else {
      RefundMails.Mail mail =
          RefundMails.rejected(inquiry.getLanguage(), inquiry.getTitle(), reason);
      emailSender.send(author.email(), mail.subject(), mail.text());
    }
    return view(inquiry, author, findPayment(inquiry));
  }

  private Inquiry lockPendingRefund(UUID inquiryId) {
    Inquiry inquiry =
        inquiryRepository
            .findWithLockByIdAndCategory(inquiryId, InquiryCategory.REFUND)
            .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));
    if (!inquiry.isRefundRequested()) {
      throw new BusinessException(ErrorCode.INQUIRY_ALREADY_DECIDED);
    }
    return inquiry;
  }

  // 사유가 비어 있으면 예외 없음으로 본다
  private static AdminRefundApproveRequest.PolicyException normalizeException(
      AdminRefundApproveRequest.PolicyException exception) {
    if (exception == null || exception.reason() == null || exception.reason().isBlank()) {
      return null;
    }
    String reason = exception.reason().strip();
    String memo =
        exception.memo() == null || exception.memo().isBlank() ? null : exception.memo().strip();
    if (reason.length() > MAX_EXCEPTION_REASON_LENGTH) {
      throw InquiryService.invalid(
          "exception.reason", "최대 " + MAX_EXCEPTION_REASON_LENGTH + "자입니다");
    }
    if (memo != null && memo.codePointCount(0, memo.length()) > MAX_EXCEPTION_MEMO_LENGTH) {
      throw InquiryService.invalid("exception.memo", "최대 " + MAX_EXCEPTION_MEMO_LENGTH + "자입니다");
    }
    if (OTHER_EXCEPTION_REASON.equals(reason) && memo == null) {
      throw InquiryService.invalid("exception.memo", "기타 사유는 메모가 필요합니다");
    }
    return new AdminRefundApproveRequest.PolicyException(reason, memo);
  }

  private PaymentRefundInfo findPayment(Inquiry inquiry) {
    return diaryPaymentService.findRefundInfos(List.of(inquiry.getPaymentId())).stream()
        .findFirst()
        .orElse(null);
  }

  private AdminRefundRequestResponse view(Inquiry inquiry) {
    return view(inquiry, inquiryAuthorLoader.load(inquiry), findPayment(inquiry));
  }

  private static AdminRefundRequestResponse view(
      Inquiry inquiry, InquiryAuthor author, PaymentRefundInfo payment) {
    Instant createdAt = inquiry.getCreatedAt();
    return AdminRefundRequestResponse.of(
        inquiry, author, payment, AdminInquiryService.ADMIN_NAME, RefundDeadline.of(createdAt));
  }
}
