package com.seoulaim.domain.inquiry.admin.dto;

/**
 * @param body 회원 메일 본문 그대로. 1~5000자
 */
public record AdminInquiryAnswerRequest(String body) {}
