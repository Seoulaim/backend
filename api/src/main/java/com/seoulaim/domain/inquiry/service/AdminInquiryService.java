package com.seoulaim.domain.inquiry.service;

import com.seoulaim.domain.inquiry.admin.dto.AdminInquiryAnswerRequest;
import com.seoulaim.domain.inquiry.admin.dto.AdminInquiryAnswerResponse;
import com.seoulaim.domain.inquiry.admin.dto.AdminInquiryListResponse;
import com.seoulaim.domain.inquiry.admin.dto.AdminInquiryMemoRequest;
import com.seoulaim.domain.inquiry.admin.dto.AdminInquiryResponse;
import com.seoulaim.domain.inquiry.admin.dto.InquiryAuthor;
import com.seoulaim.domain.inquiry.entity.Inquiry;
import com.seoulaim.domain.inquiry.entity.InquiryAnswer;
import com.seoulaim.domain.inquiry.entity.InquiryCategory;
import com.seoulaim.domain.inquiry.entity.RefundStatus;
import com.seoulaim.domain.inquiry.repository.InquiryAnswerRepository;
import com.seoulaim.domain.inquiry.repository.InquiryRepository;
import com.seoulaim.global.exception.BusinessException;
import com.seoulaim.global.exception.ErrorCode;
import com.seoulaim.global.mail.EmailSender;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 어드민 문의 관리 (환불 문의 제외 — 환불은 RefundRequestService). 답변은 회원 이메일로만 보낸다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminInquiryService {

  /** 운영자 계정이 따로 없어 답변·환불 결정자 이름은 고정이다. */
  public static final String ADMIN_NAME = "운영자";

  static final int MAX_ANSWER_LENGTH = 5000;
  static final int MAX_MEMO_LENGTH = 2000;
  static final String ANSWER_SUBJECT_PREFIX = "[Seoulaim] Re: ";

  private final InquiryRepository inquiryRepository;
  private final InquiryAnswerRepository inquiryAnswerRepository;
  private final InquiryAuthorLoader inquiryAuthorLoader;
  private final EmailSender emailSender;
  private final Clock clock;

  public AdminInquiryListResponse getInquiries() {
    List<Inquiry> inquiries =
        inquiryRepository.findAllByCategoryNotOrderByCreatedAtDescIdDesc(InquiryCategory.REFUND);
    Map<UUID, InquiryAuthor> authors = inquiryAuthorLoader.load(inquiries);
    Map<UUID, List<AdminInquiryAnswerResponse>> answers = loadAnswers(inquiries);
    return new AdminInquiryListResponse(
        inquiries.stream()
            .map(
                inquiry ->
                    AdminInquiryResponse.of(
                        inquiry,
                        authors.get(inquiry.getUserId()),
                        answers.getOrDefault(inquiry.getId(), List.of())))
            .toList(),
        inquiryRepository.countByRefundStatus(RefundStatus.REQUESTED));
  }

  public AdminInquiryResponse getInquiry(UUID inquiryId) {
    return view(findInquiry(inquiryId));
  }

  /**
   * 답변을 작성자 이메일로 보내고 기록한 뒤 ANSWERED 로 바꾼다. 메일 발송이 실패하면 아무것도 저장하지 않는다(503 AUTH_MAIL_SEND_FAILED).
   */
  @Transactional
  public AdminInquiryResponse answer(UUID inquiryId, AdminInquiryAnswerRequest request) {
    Inquiry inquiry = findInquiry(inquiryId);
    String body = request.body() == null ? "" : request.body().strip();
    if (body.isEmpty() || body.codePointCount(0, body.length()) > MAX_ANSWER_LENGTH) {
      throw InquiryService.invalid("body", "1~" + MAX_ANSWER_LENGTH + "자여야 합니다");
    }
    InquiryAuthor author = inquiryAuthorLoader.load(inquiry);
    if (author.email() == null) {
      throw InquiryService.invalid("author", "탈퇴했거나 이메일이 없는 회원이라 답변 메일을 보낼 수 없습니다");
    }
    // 언어별 템플릿 없이 어드민이 쓴 본문(문의 언어로 작성)을 그대로 보낸다
    emailSender.send(author.email(), ANSWER_SUBJECT_PREFIX + inquiry.getTitle(), body);
    inquiryAnswerRepository.save(InquiryAnswer.create(inquiry.getId(), body, clock.instant()));
    inquiry.markAnswered();
    return view(inquiry, author);
  }

  @Transactional
  public AdminInquiryResponse saveMemo(UUID inquiryId, AdminInquiryMemoRequest request) {
    Inquiry inquiry = findInquiry(inquiryId);
    String memo = request.memo() == null || request.memo().isBlank() ? null : request.memo();
    if (memo != null && memo.codePointCount(0, memo.length()) > MAX_MEMO_LENGTH) {
      throw InquiryService.invalid("memo", "최대 " + MAX_MEMO_LENGTH + "자입니다");
    }
    inquiry.updateMemo(memo);
    return view(inquiry);
  }

  // 환불 문의는 환불 관리에서만 다룬다
  private Inquiry findInquiry(UUID inquiryId) {
    return inquiryRepository
        .findByIdAndCategoryNot(inquiryId, InquiryCategory.REFUND)
        .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));
  }

  private AdminInquiryResponse view(Inquiry inquiry) {
    return view(inquiry, inquiryAuthorLoader.load(inquiry));
  }

  private AdminInquiryResponse view(Inquiry inquiry, InquiryAuthor author) {
    return AdminInquiryResponse.of(
        inquiry, author, loadAnswers(List.of(inquiry)).getOrDefault(inquiry.getId(), List.of()));
  }

  // flush 전 새 답변도 보이도록 저장소 조회 결과를 쓴다 (JPQL 조회 전 auto flush)
  private Map<UUID, List<AdminInquiryAnswerResponse>> loadAnswers(List<Inquiry> inquiries) {
    if (inquiries.isEmpty()) {
      return Map.of();
    }
    return inquiryAnswerRepository
        .findAllByInquiryIdInOrderBySentAtAscIdAsc(inquiries.stream().map(Inquiry::getId).toList())
        .stream()
        .collect(
            Collectors.groupingBy(
                InquiryAnswer::getInquiryId,
                Collectors.mapping(
                    answer -> AdminInquiryAnswerResponse.of(answer, ADMIN_NAME),
                    Collectors.toList())));
  }
}
