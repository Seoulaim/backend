package com.seoulaim.domain.inquiry.dto;

import java.util.List;
import java.util.UUID;

/**
 * 문의 작성. 검증은 서비스에서 하고 실패는 전부 INQUIRY_INVALID.
 *
 * @param category refund | payment | diary | account | report | other (대소문자 무시)
 * @param photoFileIds presigned 업로드(domain INQUIRY)한 이미지 fileId, 최대 5개
 * @param language 작성한 앱 언어 en | ko | zh | ja
 * @param paymentId category=refund 일 때 필수. 그 외 유형에서는 무시한다
 */
public record InquiryCreateRequest(
    String category,
    String title,
    String content,
    List<UUID> photoFileIds,
    String language,
    UUID paymentId) {}
